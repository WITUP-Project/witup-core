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
import br.unb.cic.witup.analysis.symbolic.expr.SymNull;
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
import java.util.concurrent.atomic.AtomicInteger;
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
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
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
  public static final String RET_PREFIX = "_ret_";
  private static final AtomicInteger GLOBAL_FRESH_COUNTER = new AtomicInteger(0);
  private final WITUpGraph cpg;
  private Set<WITUpNode> currentPathNodes = Collections.emptySet();
  private Map<WITUpNode, Integer> currentPathLastIndex = Collections.emptyMap();
  private final SummaryResolver resolver;

  public SymbolicConstraintGenerator(final WITUpGraph cpg, final SummaryResolver resolver) {
    this.cpg = cpg;
    this.resolver = resolver;
  }

  public List<SymbolicConstraint> generateSymbolicConstraints(final WITUpPath p) {
    setCurrentPath(p);
    IterationContext iterCtx = IterationContext.compute(p);
    List<SymbolicConstraint> symbolicConstraints = new ArrayList<>(iterCtx.linkPreconditions());

    for (ThrowConstraint throwConstraint : cpg.getThrowConstraints(p)) {
      StmtGraphNode n = (StmtGraphNode) throwConstraint.node().getNode();
      List<SymbolicConstraint> preconditions = new ArrayList<>();

      SymExpr symExpr;
      if (n.getStmt() instanceof JIfStmt ifStmt) {
        SymExpr ifCond = SymExpr.fromJimple(ifStmt.getCondition());
        // Rewrite self-update var uses (loop counters / accumulators) to iter-indexed
        // SymVars before substitution. Without this, the same variable name on either
        // side of a back-edge would resolve to its initial def in both places, producing
        // contradictory same-SymExpr constraints (e.g. `0 >= xs.length` asserted both
        // true and false on the same path).
        ifCond = iterCtx.rewriteAt(ifCond, throwConstraint.forwardEdgeIndex());

        SubstituteResult result = substituteWithPreconditions(ifCond, throwConstraint.node());
        symExpr = result.expr();
        preconditions.addAll(result.preconditions());
      } else if (throwConstraint.node() instanceof CaughtExceptionNode caught) {
        SymCaughtExceptionRef catchRef = new SymCaughtExceptionRef(caught.getCaughtExceptionRef());
        preconditions.add(sealCaughtExceptionRef(catchRef));
        symExpr = catchRef;
      } else {
        throw new IllegalStateException(
            "Unexpected constraint node type: " + throwConstraint.node().getClass());
      }
      symExpr = SymExpr.simplifyBoxingPatterns(symExpr);

      symbolicConstraints.addAll(preconditions);
      symbolicConstraints.add(new SymbolicConstraint(symExpr, throwConstraint.truthValue()));
    }
    // The catch-ref seal can be emitted from both the CaughtExceptionNode throw-constraint
    // path and the tightened-substitution path on the same path; keep first occurrence so
    // ordering still reads bottom-up but drop the structural duplicate.
    return new ArrayList<>(new java.util.LinkedHashSet<>(symbolicConstraints));
  }

  private void setCurrentPath(final WITUpPath p) {
    this.currentPathNodes = new HashSet<>(p.nodes());
    List<WITUpNode> forward = p.forwardNodes();
    Map<WITUpNode, Integer> lastIndex = new HashMap<>(forward.size() * 2);
    // overwrites: a node revisited under loop unrolling ends up mapped to its
    // latest occurrence — the right anchor for "latest reaching def on path".
    for (int i = 0; i < forward.size(); i++) {
      lastIndex.put(forward.get(i), i);
    }
    this.currentPathLastIndex = lastIndex;
  }

  public List<List<SymbolicConstraint>> buildThrowConstraintPaths(final WITUpNode throwNode) {
    List<WITUpPath> throwConstraintPaths = cpg.getThrowPaths(throwNode);
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
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), preconditions);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    symExpr = SymExpr.simplifyBoxingPatterns(symExpr);
    symExpr = SymExpr.stripBooleanEncoding(symExpr);
    return new SubstituteResult(symExpr, preconditions);
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

    String receiverName = SymVar.nameOf(invoke.getBase());
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
      if (!SymVar.nameOf(assign.getLeftOp()).equals(receiverName)) {
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

        return resolver.resolveCallee(lambdaSig, actuals);
      }
      // not a dynamic invoke — skip
    }

    return Optional.empty();
  }

  private SymExpr backwardSubstitute(
      final SymExpr symExpr,
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final List<SymbolicConstraint> preconditions) {

    Set<String> freeVars = new HashSet<>();
    symExpr.collectVarNames(freeVars);
    if (freeVars.isEmpty()) {
      return symExpr;
    }
    Map<String, SymExpr> env = new HashMap<>();
    collectBindings(freeVars, env, currentNode, visited, false, preconditions);
    return env.isEmpty() ? symExpr : symExpr.resolveWith(env);
  }

  private void collectBindings(
      final Set<String> freeVars,
      final Map<String, SymExpr> env,
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final boolean followIdentity,
      final List<SymbolicConstraint> preconditions) {

    if (visited.contains(currentNode)) {
      return;
    }
    visited.add(currentNode);

    Map<String, WITUpNode> latestByVar = pickLatestPathDefs(currentNode, followIdentity);

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

        Optional<ResolvedCallee> resolvedCallee = resolveCallee(rhsOp);

        if (resolvedCallee.isEmpty() && rhsOp instanceof JInterfaceInvokeExpr ifaceInvoke) {
          resolvedCallee = tryResolveLambda(ifaceInvoke, sourceNode);
        }

        if (resolvedCallee.isPresent()) {
          String definedVar = getVariableName(assign.getLeftOp());
          if (freeVars.contains(definedVar)
              && !isShadowedByLaterDef(sourceNode, definedVar, latestByVar)) {
            ResolvedCallee callee = resolvedCallee.get();
            if (callee.guardedReturn() != null) {
              SymVar freshVar =
                  SymVar.fresh(
                      RET_PREFIX + GLOBAL_FRESH_COUNTER.getAndIncrement(),
                      callee.guardedReturn().isEmpty()
                          ? SymKind.INT
                          : callee.guardedReturn().getFirst().value().getKind());
              addBinding(freeVars, env, definedVar, freshVar);
              for (GuardedExpr ge : callee.guardedReturn()) {
                preconditions.addAll(ge.preconditions());
                SymExpr eq = new SymBinOp(BinOp.EQ, freshVar, ge.value());
                SymExpr implication = encodeImplication(ge.guard(), eq);
                preconditions.add(new SymbolicConstraint(implication, true));
              }
              if (callee.throwPathConditions() != null) {
                for (List<SymbolicConstraint> throwPath : callee.throwPathConditions()) {
                  SymExpr throwGuard = encodeConjunction(throwPath);
                  preconditions.add(new SymbolicConstraint(throwGuard, false));
                }
              }
            }
            collectBindings(freeVars, env, sourceNode, visited, followIdentity, preconditions);
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
      if (!freeVars.contains(definedVar)
          || isShadowedByLaterDef(sourceNode, definedVar, latestByVar)) {
        continue;
      }
      SymExpr boundValue = resolveCaughtExceptionRef(SymExpr.fromJimple(rhsOp), sourceNode);
      if (boundValue instanceof SymCaughtExceptionRef catchRef) {
        preconditions.add(sealCaughtExceptionRef(catchRef));
      }
      addBinding(freeVars, env, definedVar, boundValue);
      collectBindings(freeVars, env, sourceNode, visited, followIdentity, preconditions);
    }
  }

  // Encodes the semantic invariant `caught_<type> <=> caught_<type>_is_null = false` as a
  // SymExpr-level constraint. The path's exceptional edge already pins the catch flag, and
  // the throw guard substitution pins the null check; without this lock, those two Z3 bool
  // consts are independent and a future change that touches one could leave the other free.
  // Translates as Z3 mkEq on two BoolExprs, which is iff.
  private static SymbolicConstraint sealCaughtExceptionRef(final SymCaughtExceptionRef ref) {
    SymExpr nonNull = new SymBinOp(BinOp.NE, ref, SymNull.INSTANCE);
    return new SymbolicConstraint(new SymBinOp(BinOp.EQ, ref, nonNull), true);
  }

  // SootUp materializes `catch (T t) { x = t; }` as a JIdentityStmt-into-stack-temp followed
  // by a JAssignStmt copying that temp into the user local. With followIdentity=false, the
  // backward walk stops at the stack temp, so a substitution of `x` lands on `stack_N` —
  // useful but not semantic. When the bound RHS is a Local whose only path-included DDG
  // source is a JIdentityStmt with JCaughtExceptionRef on the right, collapse the chain to
  // SymCaughtExceptionRef so the resulting Z3 const reads `caught_<type>_is_null` rather
  // than `stack_N_is_null`.
  private SymExpr resolveCaughtExceptionRef(final SymExpr fallback, final WITUpNode bindSource) {
    if (!(fallback instanceof SymVar)) {
      return fallback;
    }
    JCaughtExceptionRef caughtRef = null;
    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(bindSource)) {
      WITUpNode src = cpg.getEdgeSource(edge);
      if (nodeNotInPath(src)) {
        continue;
      }
      JCaughtExceptionRef ref = WITUpGraph.caughtExceptionRefOf(src);
      if (ref == null) {
        continue;
      }
      if (caughtRef != null && !caughtRef.getType().equals(ref.getType())) {
        return fallback;
      }
      caughtRef = ref;
    }
    return caughtRef != null ? new SymCaughtExceptionRef(caughtRef) : fallback;
  }

  private static boolean isShadowedByLaterDef(
      final WITUpNode src, final String var, final Map<String, WITUpNode> latestByVar) {
    WITUpNode latest = latestByVar.get(var);
    return latest != null && !src.equals(latest);
  }

  // For each variable defined by 2+ path-included DDG sources at currentNode, returns the
  // source whose forward-path position is latest — i.e. the reaching def that wins under
  // standard backward def-use chasing on this single path. Variables with a single source
  // are omitted (the existing collectBindings flow handles them without shadow filtering).
  // Sources are also excluded if collectBindings would itself skip them (cast-on-non-stack
  // assigns, identity statements when followIdentity is false, self-updates handled by
  // IterationContext) — picking such a source as "latest" would shadow an earlier eligible
  // def and silently drop the substitution.
  private Map<String, WITUpNode> pickLatestPathDefs(
      final WITUpNode currentNode, final boolean followIdentity) {
    Map<String, List<WITUpNode>> byVar = new HashMap<>();
    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
      WITUpNode src = cpg.getEdgeSource(edge);
      if (nodeNotInPath(src) || !isEligibleDefSource(src, followIdentity)) {
        continue;
      }
      String var = definedVarOf(src);
      if (var == null || isSelfUpdate(src, var)) {
        continue;
      }
      byVar.computeIfAbsent(var, k -> new ArrayList<>()).add(src);
    }
    Map<String, WITUpNode> latest = new HashMap<>();
    for (Map.Entry<String, List<WITUpNode>> entry : byVar.entrySet()) {
      List<WITUpNode> srcs = entry.getValue();
      if (srcs.size() < 2) {
        continue;
      }
      WITUpNode latestSrc = null;
      int latestIdx = -1;
      for (WITUpNode src : srcs) {
        Integer idx = currentPathLastIndex.get(src);
        if (idx != null && idx > latestIdx) {
          latestIdx = idx;
          latestSrc = src;
        }
      }
      if (latestSrc != null) {
        latest.put(entry.getKey(), latestSrc);
      }
    }
    return latest;
  }

  private static boolean isEligibleDefSource(final WITUpNode src, final boolean followIdentity) {
    if (!(src instanceof SimpleNode sn)) {
      return false;
    }
    if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
      return false;
    }
    Stmt stmt = stmtNode.getStmt();
    if (stmt instanceof JAssignStmt assign) {
      // Mirror the cast-on-non-stack skip in collectBindings.
      return isStackVariable(assign.getLeftOp()) || !(assign.getRightOp() instanceof JCastExpr);
    }
    if (stmt instanceof JIdentityStmt) {
      return followIdentity;
    }
    return false;
  }

  //  private static boolean isStackVariableValue(final Value value) {
  //    return value.toString().contains("$stack");
  //  }

  private static String definedVarOf(final WITUpNode src) {
    if (!(src instanceof SimpleNode sn)) {
      return null;
    }
    if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
      return null;
    }
    Stmt stmt = stmtNode.getStmt();
    if (stmt instanceof JAssignStmt assign) {
      return getVariableName(assign.getLeftOp());
    }
    if (stmt instanceof JIdentityStmt identity) {
      return getVariableName(identity.getLeftOp());
    }
    return null;
  }

  private static boolean isSelfUpdate(final WITUpNode src, final String lhsName) {
    if (!(src instanceof SimpleNode sn)) {
      return false;
    }
    if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
      return false;
    }
    if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
      return false;
    }
    return SymExpr.fromJimple(assign.getRightOp()).contains(lhsName);
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

  public List<GuardedExpr> traceGuardedReturn() {
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
      SubstituteResult sr =
          substituteWithPreconditions(SymExpr.fromJimple(returnNode.getOp()), returnNode);
      SymExpr value = SymExpr.stripBoxing(sr.expr());
      List<SymbolicConstraint> guard = generateSymbolicConstraints(path);
      result.add(new GuardedExpr(guard, value, sr.preconditions()));
    }
    return result;
  }

  private Optional<ResolvedCallee> resolveCallee(final Value rhsOp) {
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

    return resolver.resolveCallee(calleeSig, actuals);
  }

  private boolean nodeNotInPath(final WITUpNode node) {
    return !currentPathNodes.contains(node);
  }

  private static String getVariableName(final Value value) {
    return SymVar.nameOf(value);
  }

  private static boolean isStackVariable(final LValue value) {
    return value.toString().contains("$stack");
  }
}
