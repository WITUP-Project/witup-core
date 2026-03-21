package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.SummaryResolver;
import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.CaughtExceptionNode;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;

/**
 * Translates Jimple constraints into SymbolicConstraints. Uses backwards data flow resolution to
 * trace temporaries, parameters back to their origin node Produces a path of symbolic constraints
 * to be tested by Z3.
 */
public final class SymbolicConstraintGenerator {
  private final WITUpGraph cpg;
  private final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths;
  private Set<WITUpNode> currentPathNodes = Collections.emptySet();
  // for now, resolver being null means intraprocedural. fix me when poc is done
  private final SummaryResolver resolver;
  private static final Logger log = LoggerFactory.getLogger("SymbolicConstraintGenerator");

  public SymbolicConstraintGenerator(
      final WITUpGraph cpg, final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths) {
    this(cpg, constraintPaths, null);
  }

  /**
   * Interprocedural constructor.
   *
   * @param cpg a WITUpGraph with the CPG of the method under analysis
   * @param constraintPaths List<GraphPath<WITUpNode, WITUpEdge>> paths that represent symbolic
   *     constraints
   * @param resolver a MethodSummariser that recursively resolves interprocedural calls.
   */
  public SymbolicConstraintGenerator(
      final WITUpGraph cpg,
      final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths,
      final SummaryResolver resolver) {
    this.cpg = cpg;
    this.constraintPaths = constraintPaths;
    this.resolver = resolver;
  }

  public List<List<SymbolicConstraint>> generateSymbolicConstraintPaths() {
    List<List<SymbolicConstraint>> symbolicConstraints = new ArrayList<>();
    for (GraphPath<WITUpNode, WITUpEdge> p : this.constraintPaths) {
      List<SymbolicConstraint> resolved = generateSymbolicConstraints(p);

      if (!resolved.isEmpty()) {
        symbolicConstraints.add(resolved);
      }
    }
    return symbolicConstraints;
  }

  public List<SymbolicConstraint> generateSymbolicConstraints(
      final GraphPath<WITUpNode, WITUpEdge> p) {

    this.setCurrentPath(p);
    List<SymbolicConstraint> symbolicConstraints = new ArrayList<>();
    for (ThrowConstraint throwConstraint : cpg.getThrowConstraints(p)) {
      SymExpr symExpr = generateSymbolicExpression(throwConstraint.node());
      boolean truthValue = throwConstraint.truthValue();

      if (symExpr.getKind() == SymKind.BOOLEAN_METHOD) {
        truthValue = !truthValue;
      }

      symbolicConstraints.add(new SymbolicConstraint(symExpr, truthValue));
    }
    return symbolicConstraints;
  }

  private void setCurrentPath(final GraphPath<WITUpNode, WITUpEdge> p) {
    this.currentPathNodes = new HashSet<>(p.getVertexList());
  }

  private SymExpr substitute(final SymExpr initial, final WITUpNode startNode) {
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), false);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    return SymExpr.stripBooleanEncoding(symExpr);
  }

  public SymExpr generateSymbolicExpression(final WITUpNode constraintNode) {
    StmtGraphNode n = (StmtGraphNode) constraintNode.getNode();
    if (n.getStmt() instanceof JIfStmt ifStmt) {
      return substitute(SymExpr.fromJimple(ifStmt.getCondition()), constraintNode);
    }
    if (constraintNode instanceof CaughtExceptionNode caught) {
      return new SymCaughtExceptionRef(caught.getCaughtExceptionRef());
    }
    throw new IllegalStateException(
        "Unexpected constraint node type: " + constraintNode.getClass());
  }

  public SymExpr generateReturnExpression(final ReturnStatementNode returnNode) {
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      return SymExpr.fromJimple(returnNode.getOp());
    }
    if (paths.size() == 1) {
      setCurrentPath(paths.get(0));
      return substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);
    }

    // multiple paths to same return — fold into ITE - fits Z3 well
    setCurrentPath(paths.getLast());
    SymExpr result = substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);

    for (int i = paths.size() - 2; i >= 0; i--) {
      setCurrentPath(paths.get(i));
      SymExpr pathExpr = substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);
      SymExpr pathCondition = buildPathConditionFromPath(paths.get(i));
      result = new SymITE(pathCondition, pathExpr, result);
    }

    return result;
  }

  // effectively duplicates MethodSummariser.buildPathCondition
  // still work to do in the boundaries between here and there.
  private SymExpr buildPathConditionFromPath(final GraphPath<WITUpNode, WITUpEdge> path) {
    List<List<SymbolicConstraint>> generated =
        new SymbolicConstraintGenerator(cpg, List.of(path), null).generateSymbolicConstraintPaths();

    if (generated.isEmpty() || generated.get(0).isEmpty()) {
      return SymIntConst.one();
    }

    List<SymbolicConstraint> constraints = generated.get(0);
    SymExpr result = SymIntConst.one();
    for (int i = constraints.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = constraints.get(i);
      SymExpr cond =
          c.getTruthValue()
              ? c.getSymExpr()
              : new SymBinOp(BinOp.EQ, c.getSymExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.zero());
    }
    return result;
  }

  private Optional<SymExpr> tryResolveLambda(
          final JInterfaceInvokeExpr invoke,
          final WITUpNode node) {
    if (resolver == null) {
      return Optional.empty();
    }

    String receiverName = invoke.getBase().toString();

    log.debug("tryResolveLambda: receiver={} incoming edges={}",
            receiverName, cpg.getIncomingDDGEdges(node).size());
    cpg.getIncomingDDGEdges(node).forEach(e ->
            log.debug("  edge source: {}", cpg.getEdgeSource(e)));

    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(node)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);
      if (!isNodeInPath(sourceNode)) {
        continue;
      }
      if (!(sourceNode instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      if (!assign.getLeftOp().toString().equals(receiverName)) {
        continue;
      }

      if (assign.getRightOp() instanceof JDynamicInvokeExpr dynInvoke) {
        List<Immediate> bootstrapArgs = dynInvoke.getBootstrapArgs();
        if (bootstrapArgs.size() < 2) {
          return Optional.empty();
        }
        if (!(bootstrapArgs.get(1) instanceof MethodHandle mh)) {
          return Optional.empty();
        }

        String className = mh.getReferenceSignature().getDeclClassType()
                .getFullyQualifiedName();
        String subSig = mh.getReferenceSignature().getSubSignature().toString();
        String lambdaSig = "<" + className + ": " + subSig + ">";

        List<SymExpr> actuals = dynInvoke.getArgs().stream()
                .map(SymExpr::fromJimple).collect(Collectors.toList());

        log.debug("Lambda resolution: {} with actuals {}", lambdaSig, actuals);
        return resolver.resolveReturnExpr(lambdaSig, actuals);
      }
      // not a dynamic invoke — skip
    }

    return Optional.empty();
  }

  // it's ok to reassign current in a recursive function
  // given a Jimple statement, produces a SymExpr by backwards
  // tracing temporaries back to their origins
  private SymExpr backwardSubstitute(
      SymExpr symExpr, // SUPPRESS CHECKSTYLE FinalParameters
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final boolean followIdentity) {

    if (visited.contains(currentNode)) {
      return symExpr;
    }
    visited.add(currentNode);

    Set<String> freeVars = new VariableCollector().collect(symExpr);
    log.debug("backwardSubstitute freeVars={} at node={}", freeVars, currentNode);

    if (freeVars.isEmpty()) {
      return symExpr;
    }

    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);

      if (!isNodeInPath(sourceNode)) {
        continue;
      }

      if (!(sourceNode instanceof SimpleNode simpleNode)) {
        continue;
      }

      if (!(simpleNode.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }

      Stmt stmt = stmtNode.getStmt();

      Value lhsOp;
      Value rhsOp;

      if (stmt instanceof JAssignStmt assign) {
        if (!isStackVariable(assign.getLeftOp()) && assign.getRightOp() instanceof JCastExpr) {
          continue;
        }
        rhsOp = assign.getRightOp();
        lhsOp = assign.getLeftOp();

        // this is the interprocedural hook.
        // need to add hooks for other invokes
        Optional<SymExpr> resolved = tryResolveInterprocedural(rhsOp);

        if (resolved.isEmpty() && rhsOp instanceof JInterfaceInvokeExpr ifaceInvoke) {
          log.debug("trying lambda resolution for: {}", ifaceInvoke);
          resolved = tryResolveLambda(ifaceInvoke, sourceNode);
        }

        if (resolved.isPresent()) {
          String definedVar = getVariableName(assign.getLeftOp());
          if (freeVars.contains(definedVar)) {
            symExpr = symExpr.substitute(definedVar, resolved.get());
            log.debug("after substitute: freeVars={} symExpr={}",
                    new VariableCollector().collect(symExpr), symExpr);
            symExpr = backwardSubstitute(symExpr, sourceNode, visited, followIdentity);
          }
          continue;
        }
      } else if (stmt instanceof JIdentityStmt identity) {
        if (!followIdentity) {
          continue;
        }
        lhsOp = identity.getLeftOp();
        rhsOp = identity.getRightOp();
      } else {
        continue;
      }
      // local variable on the lhs e.g. $stack1 == 0
      String definedVar = getVariableName(lhsOp);

      if (!freeVars.contains(definedVar)) {
        continue;
      }

      SymExpr rhsSymExpr = SymExpr.fromJimple(rhsOp);

      symExpr = symExpr.substitute(definedVar, rhsSymExpr);
      symExpr = backwardSubstitute(symExpr, sourceNode, visited, followIdentity);
    }

    return symExpr;
  }

  private Optional<SymExpr> tryResolveInterprocedural(final Value rhsOp) {
    if (resolver == null) {
      return Optional.empty();
    }

    log.debug("tryResolveInterprocedural: {}", rhsOp.getClass().getSimpleName());

    String calleeSig;
    List<SymExpr> actuals;

    switch (rhsOp) {
      case JVirtualInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream()
                .map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case JStaticInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream()
                .map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case JInterfaceInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream()
                .map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case JSpecialInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream()
                .map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case JDynamicInvokeExpr invoke -> {
        log.debug("DynamicInvoke: method={} bootstrapArgs={}",
                invoke.getMethodSignature(),
                invoke.getBootstrapArgs());
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream()
                .map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case null, default -> {
        return Optional.empty();
      }
    }

    log.debug("Interprocedural hook fired for: {}", calleeSig);
    return resolver.resolveReturnExpr(calleeSig, actuals);
  }

  private boolean isNodeInPath(final WITUpNode node) {
    return currentPathNodes.contains(node);
  }

  private static String getVariableName(final Value value) {
    return value.toString();
  }

  private boolean isStackVariable(final LValue value) {
    return value.toString().contains("$stack");
  }
}
