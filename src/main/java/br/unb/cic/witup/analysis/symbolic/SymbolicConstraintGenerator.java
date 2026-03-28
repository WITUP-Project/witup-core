package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.ResolvedCallee;
import br.unb.cic.witup.analysis.SummaryResolver;
import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.WITUpPath;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
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
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
  private int freshVarCounter = 0;
  public static final int MAX_THROW_FREE_PATHS = 1;

  public SymbolicConstraintGenerator(final WITUpGraph cpg, final SummaryResolver resolver) {
    this.cpg = cpg;
    this.resolver = resolver;
  }

  public List<SymbolicConstraint> generateSymbolicConstraints(final WITUpPath p) {
    setCurrentPath(p);
    List<SymbolicConstraint> symbolicConstraints = new ArrayList<>();
    for (ThrowConstraint throwConstraint : cpg.getThrowConstraints(p)) {
      StmtGraphNode n = (StmtGraphNode) throwConstraint.node().getNode();
      List<SymbolicConstraint> preconditions = new ArrayList<>();

      SymExpr symExpr;
      if (n.getStmt() instanceof JIfStmt ifStmt) {
        SubstituteResult result =
            substituteWithPreconditions(
                SymExpr.fromJimple(ifStmt.getCondition()), throwConstraint.node());
        symExpr = result.expr();
        preconditions.addAll(result.preconditions());
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
      symbolicConstraints.addAll(preconditions);
      symbolicConstraints.add(new SymbolicConstraint(symExpr, truthValue));
    }
    return symbolicConstraints;
  }

  private void setCurrentPath(final WITUpPath p) {
    this.currentPathNodes = new HashSet<>(p.nodes());
  }

  public List<List<SymbolicConstraint>> buildNodeConstraintPaths(final WITUpNode throwNode) {
    List<WITUpPath> throwConstraintPaths = cpg.getConstraintPaths(throwNode);
    return generateThrowConstraintPath(throwConstraintPaths);
  }

  private List<List<SymbolicConstraint>> generateThrowConstraintPath(
      final List<WITUpPath> throwConstraintPaths) {
    List<List<SymbolicConstraint>> symbolicConstraints = new ArrayList<>();
    for (WITUpPath p : throwConstraintPaths) {
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

  private SymExpr backwardSubstitute(
      final SymExpr symExpr,
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final boolean followIdentity,
      final List<SymbolicConstraint> extraConstraints) {

    Set<String> freeVars = new HashSet<>();
    symExpr.collectVarNames(freeVars);
    if (freeVars.isEmpty()) {
      return symExpr;
    }
    Map<String, SymExpr> env = new HashMap<>();
    collectBindings(freeVars, env, currentNode, visited, followIdentity, extraConstraints);
    return env.isEmpty() ? symExpr : symExpr.resolveWith(env);
  }

  private void collectBindings(
      final Set<String> freeVars,
      final Map<String, SymExpr> env,
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final boolean followIdentity,
      final List<SymbolicConstraint> extraConstraints) {

    if (visited.contains(currentNode)) {
      return;
    }
    visited.add(currentNode);

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

        Optional<ResolvedCallee> resolved = tryResolveInterprocedural(rhsOp);

        if (resolved.isEmpty() && rhsOp instanceof JInterfaceInvokeExpr ifaceInvoke) {
          resolved = tryResolveLambda(ifaceInvoke, sourceNode);
        }

        if (resolved.isPresent()) {
          String definedVar = getVariableName(assign.getLeftOp());
          if (freeVars.contains(definedVar)) {
            ResolvedCallee callee = resolved.get();
            if (callee.guardedReturn() != null) {
              SymVar freshVar =
                  SymVar.fresh(
                      "_ret_" + freshVarCounter++,
                      callee.guardedReturn().isEmpty()
                          ? SymKind.INT
                          : callee.guardedReturn().getFirst().value().getKind());
              addBinding(freeVars, env, definedVar, freshVar);
              for (GuardedExpr ge : callee.guardedReturn()) {
                SymExpr eq = new SymBinOp(BinOp.EQ, freshVar, ge.value());
                SymExpr implication = encodeImplication(ge.guard(), eq);
                extraConstraints.add(new SymbolicConstraint(implication, true));
              }
              if (callee.throwPathConditions() != null) {
                for (List<SymbolicConstraint> throwPath : callee.throwPathConditions()) {
                  SymExpr throwGuard = encodeConjunction(throwPath);
                  extraConstraints.add(new SymbolicConstraint(throwGuard, false));
                }
              }
            }
            collectBindings(freeVars, env, sourceNode, visited, followIdentity, extraConstraints);
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

      String definedVar = getVariableName(lhsOp);

      if (!freeVars.contains(definedVar)) {
        continue;
      }

      SymExpr rhsSymExpr = SymExpr.fromJimple(rhsOp);

      addBinding(freeVars, env, definedVar, rhsSymExpr);
      collectBindings(freeVars, env, sourceNode, visited, followIdentity, extraConstraints);
    }
  }

  private static void addBinding(
      final Set<String> freeVars,
      final Map<String, SymExpr> env,
      final String varName,
      final SymExpr expr) {
    SymExpr existing = env.get(varName);
    if (existing != null) {
      // variable redefined on the path — compose with the earlier binding
      env.put(varName, existing.substitute(varName, expr));
    } else {
      env.put(varName, expr);
    }
    freeVars.remove(varName);
    expr.collectVarNames(freeVars);
  }

  private static SymExpr encodeImplication(
      final List<SymbolicConstraint> guard, final SymExpr consequent) {
    if (guard.isEmpty()) {
      return consequent;
    }
    SymExpr result = consequent;
    for (int i = guard.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = guard.get(i);
      SymExpr cond =
          c.truthValue() ? c.symExpr() : new SymBinOp(BinOp.EQ, c.symExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.one());
    }
    return result;
  }

  private static SymExpr encodeConjunction(final List<SymbolicConstraint> constraints) {
    SymExpr result = SymIntConst.one();
    for (int i = constraints.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = constraints.get(i);
      SymExpr cond =
          c.truthValue() ? c.symExpr() : new SymBinOp(BinOp.EQ, c.symExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.zero());
    }
    return result;
  }

  public List<GuardedExpr> traceReturnGuarded() {
    List<ReturnStatementNode> returnNodes = cpg.getReturnNodes();
    if (returnNodes.isEmpty()) {
      return List.of();
    }

    List<GuardedExpr> result = new ArrayList<>();
    for (int i = returnNodes.size() - 1; i >= 0; i--) {
      result.addAll(generateReturnGuarded(returnNodes.get(i)));
    }
    return result;
  }

  private List<GuardedExpr> generateReturnGuarded(final ReturnStatementNode returnNode) {
    List<WITUpPath> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      SymExpr value = SymExpr.stripBoxing(SymExpr.fromJimple(returnNode.getOp()));
      return List.of(new GuardedExpr(List.of(), value));
    }

    List<GuardedExpr> result = new ArrayList<>(paths.size());
    for (WITUpPath path : paths) {
      setCurrentPath(path);
      SymExpr value = substitute(SymExpr.fromJimple(returnNode.getOp()), returnNode);
      value = SymExpr.stripBoxing(value);
      List<SymbolicConstraint> guard = generateSymbolicConstraints(path);
      result.add(new GuardedExpr(guard, value));
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
