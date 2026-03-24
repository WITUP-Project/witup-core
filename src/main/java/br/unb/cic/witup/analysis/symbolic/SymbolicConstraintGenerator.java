package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.ResolvedCallee;
import br.unb.cic.witup.analysis.SummaryResolver;
import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.CaughtExceptionNode;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymCaughtExceptionRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymITE;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
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
import sootup.core.types.Type;

/**
 * Translates Jimple constraints into SymbolicConstraints. Uses backwards data flow resolution to
 * trace temporaries, parameters back to their origin node Produces a path of symbolic constraints
 * to be tested by Z3.
 */
public final class SymbolicConstraintGenerator {
  private final WITUpGraph cpg;
  private Set<WITUpNode> currentPathNodes = Collections.emptySet();
  // for now, resolver being null means intraprocedural. fix me when poc is done
  private final SummaryResolver resolver;
  public static final int MAX_THROW_FREE_PATHS = 10000;

  public SymbolicConstraintGenerator(final WITUpGraph cpg, final SummaryResolver resolver) {
    this.cpg = cpg;
    this.resolver = resolver;
  }

  public List<SymbolicConstraint> generateSymbolicConstraints(
      final GraphPath<WITUpNode, WITUpEdge> p) {

    this.setCurrentPath(p);
    List<SymbolicConstraint> symbolicConstraints = new ArrayList<>();
    for (ThrowConstraint throwConstraint : cpg.getThrowConstraints(p)) {
      StmtGraphNode n = (StmtGraphNode) throwConstraint.node().getNode();

      List<SymbolicConstraint> extraConstraints = new ArrayList<>();
      SymExpr symExpr;

      if (n.getStmt() instanceof JIfStmt ifStmt) {
        SubstituteResult result =
            substituteWithPreconditions(
                SymExpr.fromJimple(ifStmt.getCondition()), throwConstraint.node());
        symExpr = result.expr();
        extraConstraints.addAll(result.preconditions());
      } else if (throwConstraint.node() instanceof CaughtExceptionNode caught) {
        symExpr = new SymCaughtExceptionRef(caught.getCaughtExceptionRef());
      } else {
        throw new IllegalStateException(
            "Unexpected constraint node type: " + throwConstraint.node().getClass());
      }
      symExpr = SymExpr.simplifyBoxingPatterns(symExpr);

      boolean truthValue = throwConstraint.truthValue();
      if (symExpr.getKind() == SymKind.BOOLEAN_METHOD) {
        truthValue = !truthValue;
      }
      symbolicConstraints.addAll(extraConstraints);
      symbolicConstraints.add(new SymbolicConstraint(symExpr, truthValue));
    }
    return symbolicConstraints;
  }

  private void setCurrentPath(final GraphPath<WITUpNode, WITUpEdge> p) {
    this.currentPathNodes = new HashSet<>(p.getVertexList());
  }

  public List<List<SymbolicConstraint>> buildNodeConstraintPaths(final WITUpNode throwNode) {
    List<GraphPath<WITUpNode, WITUpEdge>> throwConstraintPaths = cpg.getConstraintPaths(throwNode);
    return generateThrowConstraintPath(throwConstraintPaths);
  }

  private List<List<SymbolicConstraint>> generateThrowConstraintPath(
      final List<GraphPath<WITUpNode, WITUpEdge>> throwConstraintPaths) {
    List<List<SymbolicConstraint>> symbolicConstraints = new ArrayList<>();
    for (GraphPath<WITUpNode, WITUpEdge> p : throwConstraintPaths) {
      List<SymbolicConstraint> resolved = generateSymbolicConstraints(p);

      if (!resolved.isEmpty()) {
        symbolicConstraints.add(resolved);
      }
    }
    return symbolicConstraints;
  }

  private record SubstituteResult(SymExpr expr, List<SymbolicConstraint> preconditions) {}

  private SubstituteResult substituteWithPreconditions(
      final SymExpr initial, final WITUpNode startNode) {
    List<SymbolicConstraint> preconditions = new ArrayList<>();
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), false, preconditions);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    symExpr = SymExpr.simplifyBoxingPatterns(symExpr);
    symExpr = SymExpr.stripBooleanEncoding(symExpr);
    return new SubstituteResult(symExpr, preconditions);
  }

  private SymExpr substitute(final SymExpr initial, final WITUpNode startNode) {
    // when substituting inside, no need to consider constraints from other methods
    List<SymbolicConstraint> ignored = new ArrayList<>();
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), false, ignored);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    symExpr = SymExpr.simplifyBoxingPatterns(symExpr);
    return SymExpr.stripBooleanEncoding(symExpr);
  }

  public SymExpr generateReturnExpression(final ReturnStatementNode returnNode) {
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      return SymExpr.fromJimple(returnNode.getOp());
    }
    if (paths.size() == 1) {
      setCurrentPath(paths.getFirst());
      return substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);
    }

    // multiple paths to same return — fold into ITE - fits Z3 well
    setCurrentPath(paths.getLast());
    SymExpr result = substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);

    for (int i = paths.size() - 2; i >= 0; i--) {
      setCurrentPath(paths.get(i));
      SymExpr pathExpr = substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);
      SymExpr pathCondition = buildPathConditionExpr(paths.get(i));
      result = new SymITE(pathCondition, pathExpr, result);
    }

    return result;
  }

  private SymExpr buildPathConditionExpr(final GraphPath<WITUpNode, WITUpEdge> path) {
    List<List<SymbolicConstraint>> generated = generateThrowConstraintPath(List.of(path));

    if (generated.isEmpty() || generated.getFirst().isEmpty()) {
      return SymIntConst.one();
    }

    List<SymbolicConstraint> constraints = generated.getFirst();
    return generatePathConditionExpr(constraints);
  }

  // if a callee returned, it means one of its return paths was reached
  // for each path, build the negation of its conjunction
  // throw-free means: NOT(path1) AND NOT(path2) AND ...
  // NOT(path) = NOT(c1 AND c2 AND ...) = NOT(c1) OR NOT(c2) OR ...
  // but for simplicity encode as ITE tree (may cost a lot of memory)
  // throw-free precondition: all throw paths are false
  // encode as: ITE(throwCond1, 0, ITE(throwCond2, 0, 1))
  public SymExpr buildThrowFreePrecondition(final List<List<SymbolicConstraint>> paths) {
    if (paths == null || paths.isEmpty()) {
      return null;
    }
    List<List<SymbolicConstraint>> boundedThrowFreePaths =
        paths.size() > MAX_THROW_FREE_PATHS ? paths.subList(0, MAX_THROW_FREE_PATHS) : paths;

    SymExpr result = SymIntConst.one();
    for (List<SymbolicConstraint> path : boundedThrowFreePaths) {
      SymExpr pathCond = generatePathConditionExpr(path);
      result = new SymITE(pathCond, SymIntConst.zero(), result);
    }
    return result;
  }

  public SymExpr generatePathConditionExpr(final List<SymbolicConstraint> constraints) {
    SymExpr result = SymIntConst.one();
    // traverse constraints backwards to build the recursive ITE
    for (int i = constraints.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = constraints.get(i);
      SymExpr cond =
          c.truthValue() ? c.symExpr() : new SymBinOp(BinOp.EQ, c.symExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.zero());
    }
    return result;
  }

  public List<SymParamRef> buildFormals() {
    List<Type> paramTypes = cpg.getMethod().getParameterTypes();
    List<SymParamRef> formals = new ArrayList<>();
    for (int i = 0; i < paramTypes.size(); i++) {
      formals.add(new SymParamRef(i, paramTypes.get(i)));
    }
    // @this in -1 index
    if (!cpg.getMethod().isStatic()) {
      formals.add(new SymParamRef(-1, cpg.getMethod().getDeclaringClassType()));
    }
    return formals;
  }

  private Optional<ResolvedCallee> tryResolveLambda(
      final JInterfaceInvokeExpr invoke, final WITUpNode node) {
    if (resolver == null) {
      return Optional.empty();
    }

    String receiverName = invoke.getBase().toString();

    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(node)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);
      if (nodeNotInPath(sourceNode)) {
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
        // this should always be the implementation method handle for lambda
        // metafactory invocations
        if (!(bootstrapArgs.get(1) instanceof MethodHandle mh)) {
          return Optional.empty();
        }

        // e.g. <Int: Integer lambda$applyAndCheckResult$1(int)>
        String className = mh.getReferenceSignature().getDeclClassType().getFullyQualifiedName();
        String subSig = mh.getReferenceSignature().getSubSignature().toString();
        String lambdaSig = "<" + className + ": " + subSig + ">";

        List<SymExpr> actuals =
            dynInvoke.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());

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
      final boolean followIdentity,
      final List<SymbolicConstraint> extraConstraints) {

    if (visited.contains(currentNode)) {
      return symExpr;
    }
    visited.add(currentNode);

    Set<String> freeVars = new VariableCollector().collect(symExpr);

    if (freeVars.isEmpty()) {
      return symExpr;
    }

    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);
      if (nodeNotInPath(sourceNode)) {
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
        Optional<ResolvedCallee> resolved = tryResolveInterprocedural(rhsOp);

        if (resolved.isEmpty() && rhsOp instanceof JInterfaceInvokeExpr ifaceInvoke) {
          resolved = tryResolveLambda(ifaceInvoke, sourceNode);
        }

        if (resolved.isPresent()) {
          String definedVar = getVariableName(assign.getLeftOp());
          if (freeVars.contains(definedVar)) {
            symExpr = symExpr.substitute(definedVar, resolved.get().returnExpr());
            // add callee's throw-free precondition as extra constraint
            if (resolved.get().precondition() != null) {
              extraConstraints.add(new SymbolicConstraint(resolved.get().precondition(), true));
            }
            symExpr =
                backwardSubstitute(symExpr, sourceNode, visited, followIdentity, extraConstraints);
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
      symExpr = backwardSubstitute(symExpr, sourceNode, visited, followIdentity, extraConstraints);
    }

    return symExpr;
  }

  public SymExpr buildPathCondition(final ReturnStatementNode returnNode) {
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      return SymIntConst.one();
    }

    // build a condition per path, then disjoin them
    // ITE(cond1, 1, ITE(cond2, 1, ITE(cond3, 1, 0)))
    SymExpr result = SymIntConst.zero();

    for (int p = paths.size() - 1; p >= 0; p--) {
      List<List<SymbolicConstraint>> generated = generateThrowConstraintPath(List.of(paths.get(p)));

      if (generated.isEmpty() || generated.getFirst().isEmpty()) {
        return SymIntConst.one(); // unconditional path exists — always reachable
      }
      List<SymbolicConstraint> constraints = generated.getFirst();
      SymExpr pathCond = generatePathConditionExpr(constraints);
      // disjoin: if this path's condition holds, result is 1
      result = new SymITE(pathCond, SymIntConst.one(), result);
    }

    return result;
  }

  public SymExpr traceReturnExpr() {
    List<ReturnStatementNode> returnNodes = cpg.getReturnNodes();
    if (returnNodes.isEmpty()) {
      return null;
    }

    SymExpr result = null;

    for (ReturnStatementNode returnNode : returnNodes) {
      SymExpr returnExpr = generateReturnExpression(returnNode);
      if (returnExpr == null) {
        continue;
      }

      if (result == null) {
        // base case — last return in iteration becomes the else branch
        result = returnExpr;
      } else {
        SymExpr pathCondition = buildPathCondition(returnNode);
        result = new SymITE(pathCondition, returnExpr, result);
      }
    }

    return result;
  }

  private Optional<ResolvedCallee> tryResolveInterprocedural(final Value rhsOp) {
    if (resolver == null) {
      return Optional.empty();
    }

    String calleeSig;
    List<SymExpr> actuals;

    switch (rhsOp) {
      case JVirtualInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = new ArrayList<>();
        invoke.getArgs().stream().map(SymExpr::fromJimple).forEach(actuals::add);
        actuals.add(SymExpr.fromJimple(invoke.getBase()));
      }
      case JStaticInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case JInterfaceInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = new ArrayList<>();
        invoke.getArgs().stream().map(SymExpr::fromJimple).forEach(actuals::add);
        actuals.add(SymExpr.fromJimple(invoke.getBase())); // add @this
      }
      case JSpecialInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = new ArrayList<>();
        invoke.getArgs().stream().map(SymExpr::fromJimple).forEach(actuals::add);
        actuals.add(SymExpr.fromJimple(invoke.getBase())); // add @this
      }
      case JDynamicInvokeExpr invoke -> {
        calleeSig = invoke.getMethodSignature().toString();
        actuals = invoke.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());
      }
      case null, default -> {
        return Optional.empty();
      }
    }

    return resolver.resolveReturnExpr(calleeSig, actuals);
  }

  private boolean nodeNotInPath(final WITUpNode node) {
    return !currentPathNodes.contains(node);
  }

  private static String getVariableName(final Value value) {
    return value.toString();
  }

  private boolean isStackVariable(final LValue value) {
    return value.toString().contains("$stack");
  }
}
