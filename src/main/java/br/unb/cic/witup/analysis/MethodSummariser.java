package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.ImplicitAioobeSite;
import br.unb.cic.witup.analysis.graph.ImplicitArithmeticSite;
import br.unb.cic.witup.analysis.graph.ImplicitNegativeArraySizeSite;
import br.unb.cic.witup.analysis.graph.ImplicitNpeReceiverSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.ForwardPathAnalysis;
import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
import br.unb.cic.witup.analysis.symbolic.PathConditionIndex;
import br.unb.cic.witup.analysis.symbolic.PathFact;
import br.unb.cic.witup.analysis.symbolic.SymExprResolver;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraintGenerator;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymLength;
import br.unb.cic.witup.analysis.symbolic.expr.SymNull;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a method that throws, build the symbolic constraints for each path leading to throw nodes.
 */
public final class MethodSummariser implements SummaryResolver {
  private static final Logger log = LoggerFactory.getLogger(MethodSummariser.class);
  private final WITUpGraph cpg;
  private final GraphRepository graphRepository;
  private final SummaryRepository summaryRepository;
  private final SymbolicConstraintGenerator symbolicConstraintGenerator;
  private final Map<InstantiationKey, Optional<ResolvedCallee>> instantiationCache =
      new HashMap<>();
  private final boolean emitImplicitExceptions;
  private PathConditionIndex pathConditions;

  /**
   * Interprocedural MethodSummariser
   *
   * @param cpg WITUpGraph of the method being analysed
   * @param graphRepository GraphRepository
   * @param summaryRepository SummaryRepository
   * @param emitImplicitExceptions when true, synthesise rollup-level ExceptionPaths beyond the
   *     method's own athrow sites: IMPLICIT-kind paths for JVM implicit exceptions (NPE, AIOOBE,
   *     NegativeArraySize, ArithmeticException) and CALLEE_PROPAGATED-kind paths for uncaught
   *     escapes from callees. Both classes are gated together. Legacy tests remain on the
   *     default-off branch for now — they'll be revisited so they reflect the new synthesis when we
   *     trust it.
   */
  public MethodSummariser(
      final WITUpGraph cpg,
      final GraphRepository graphRepository,
      final SummaryRepository summaryRepository,
      final boolean emitImplicitExceptions) {
    this.cpg = cpg;
    this.graphRepository = graphRepository;
    this.summaryRepository = summaryRepository;
    this.emitImplicitExceptions = emitImplicitExceptions;
    this.symbolicConstraintGenerator = new SymbolicConstraintGenerator(cpg, this);
  }

  /** Recursively produces MethodSummary. */
  public MethodSummary summarise() {
    String sig = cpg.getMethodSignature();

    Optional<MethodSummary> cached = summaryRepository.getSummary(sig);
    if (cached.isPresent()) {
      return cached.get();
    }
    summaryRepository.markInProgress(sig);

    List<ExceptionPath> exceptionPaths = new ArrayList<>();
    List<List<SymbolicConstraint>> throwConstraintPaths = new ArrayList<>();
    for (WITUpNode throwNode : cpg.getThrowNodes()) {
      ThrowStatementNode throwStmt = (ThrowStatementNode) throwNode;
      // javac synthesises a catch-all rethrow for `finally` and try-with-resources. It
      // re-raises whatever the guarded region already threw — already reported as an
      // own-throw or CALLEE_PROPAGATED path — so emitting it double counts that exception
      // under java.lang.Throwable, a type no caller catches, once per enumerated path.
      if (cpg.isSyntheticCatchAllRethrow(throwStmt)) {
        log.debug("skipping synthetic catch-all rethrow in {}", sig);
        continue;
      }
      String exceptionQualifiedName = cpg.resolveExceptionType(throwStmt);
      ThrowSiteKind throwSiteKind = cpg.classifyThrowSite(throwStmt);
      // resolveExceptionType only handles `throw new X()`; for `throw caughtVar` (rethrow)
      // it returns null. Fall back to the surrounding catch handler's declared type so
      // the row is actually identifiable in benchmark matching.
      if (exceptionQualifiedName == null && throwSiteKind == ThrowSiteKind.RETHROW) {
        exceptionQualifiedName = cpg.resolveRethrowCaughtType(throwStmt);
      }
      for (List<SymbolicConstraint> constraints :
          symbolicConstraintGenerator.buildThrowConstraintPaths(throwNode)) {
        exceptionPaths.add(
            new ExceptionPath(
                constraints, throwNode, exceptionQualifiedName, throwSiteKind, List.of()));
        throwConstraintPaths.add(constraints);
      }
    }

    if (emitImplicitExceptions) {
      pathConditions = analysePathConditions();
      collectImplicitNpePaths(exceptionPaths, throwConstraintPaths);
      collectImplicitAioobePaths(exceptionPaths, throwConstraintPaths);
      collectImplicitNegativeArraySizePaths(exceptionPaths, throwConstraintPaths);
      collectImplicitArithmeticPaths(exceptionPaths, throwConstraintPaths);
    }
    // Callee-propagated paths are no longer materialised in the summary itself; the
    // ExceptionFlowWalker composes them on demand from this method's call sites and
    // catch handlers (both already exposed by WITUpGraph).

    List<SymParamRef> formals = symbolicConstraintGenerator.buildFormals();
    List<GuardedExpr> guardedReturn = symbolicConstraintGenerator.traceGuardedReturn();
    MethodSummary summary =
        new MethodSummary(
            cpg.getMethodSignature(), exceptionPaths, formals, guardedReturn, throwConstraintPaths);

    summaryRepository.putSummary(sig, summary);
    return summary;
  }

  // synthesise npe at potential throw sites
  private void collectImplicitNpePaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitNpeReceiverSite site : cpg.getImplicitNpeReceiverSites()) {
      for (Optional<PathFact> fact : factsAt(site.node())) {
        SymExpr receiver = resolveOperand(SymExpr.fromJimple(site.receiver()), fact, site.node());
        emitImplicitPath(
            exceptionPaths,
            throwConstraintPaths,
            fact,
            site.node(),
            new SymbolicConstraint(new SymBinOp(BinOp.EQ, receiver, SymNull.INSTANCE), true),
            "java.lang.NullPointerException");
      }
    }
  }

  // For each array element access, assert `arr != null AND (i < 0 || i >= arr.length)`. The
  // non-null conjunct separates this from the NPE-on-the-array-base case, since the JVM raises
  // that one first when both could fire.
  private void collectImplicitAioobePaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitAioobeSite site : cpg.getImplicitAioobeSites()) {
      for (Optional<PathFact> fact : factsAt(site.node())) {
        SymExpr array = resolveOperand(SymExpr.fromJimple(site.arrayBase()), fact, site.node());
        SymExpr index = resolveOperand(SymExpr.fromJimple(site.index()), fact, site.node());
        SymExpr nonNull = new SymBinOp(BinOp.NE, array, SymNull.INSTANCE);
        SymExpr belowZero = new SymBinOp(BinOp.LT, index, SymIntConst.zero());
        SymExpr pastEnd = new SymBinOp(BinOp.GE, index, new SymLength(array));
        emitImplicitPath(
            exceptionPaths,
            throwConstraintPaths,
            fact,
            site.node(),
            new SymbolicConstraint(
                new SymBinOp(BinOp.AND, nonNull, new SymBinOp(BinOp.OR, belowZero, pastEnd)), true),
            "java.lang.ArrayIndexOutOfBoundsException");
      }
    }
  }

  // For each `new T[n]`, assert that the size was negative.
  private void collectImplicitNegativeArraySizePaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitNegativeArraySizeSite site : cpg.getImplicitNegativeArraySizeSites()) {
      for (Optional<PathFact> fact : factsAt(site.node())) {
        SymExpr size = resolveOperand(SymExpr.fromJimple(site.size()), fact, site.node());
        emitImplicitPath(
            exceptionPaths,
            throwConstraintPaths,
            fact,
            site.node(),
            new SymbolicConstraint(new SymBinOp(BinOp.LT, size, SymIntConst.zero()), true),
            "java.lang.NegativeArraySizeException");
      }
    }
  }

  // For each integer `/` or `%`, assert that the divisor was zero.
  private void collectImplicitArithmeticPaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitArithmeticSite site : cpg.getImplicitArithmeticSites()) {
      for (Optional<PathFact> fact : factsAt(site.node())) {
        SymExpr divisor = resolveOperand(SymExpr.fromJimple(site.divisor()), fact, site.node());
        emitImplicitPath(
            exceptionPaths,
            throwConstraintPaths,
            fact,
            site.node(),
            new SymbolicConstraint(new SymBinOp(BinOp.EQ, divisor, SymIntConst.zero()), true),
            "java.lang.ArithmeticException");
      }
    }
  }

  // The site check, under the conditions that reached it.
  private void emitImplicitPath(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths,
      final Optional<PathFact> fact,
      final WITUpNode node,
      final SymbolicConstraint check,
      final String exceptionQualifiedName) {
    List<SymbolicConstraint> predicate = new ArrayList<>();
    fact.ifPresent(f -> predicate.addAll(f.pc().toList()));
    predicate.add(check);
    List<SymbolicConstraint> filtered =
        SymbolicConstraintGenerator.foldAndFilterConstraints(predicate);
    if (filtered == null) {
      return;
    }
    exceptionPaths.add(
        new ExceptionPath(
            filtered, node, exceptionQualifiedName, ThrowSiteKind.IMPLICIT, List.of()));
    throwConstraintPaths.add(filtered);
  }

  private SymExpr resolveOperand(
      final SymExpr operand, final Optional<PathFact> fact, final WITUpNode node) {
    return fact.map(f -> SymbolicConstraintGenerator.foldConstants(operand.resolveWith(f.env())))
        .orElseGet(() -> SymExprResolver.resolveLocalAt(operand, node, cpg));
  }

  // One entry per way of reaching the site
  private List<Optional<PathFact>> factsAt(final WITUpNode node) {
    List<PathFact> facts = pathConditions.factsAt(node);
    if (facts.isEmpty()) {
      return List.of(Optional.empty());
    }
    return facts.stream().map(Optional::of).toList();
  }

  private PathConditionIndex analysePathConditions() {
    Set<WITUpNode> sites = new LinkedHashSet<>();
    for (ImplicitNpeReceiverSite site : cpg.getImplicitNpeReceiverSites()) {
      sites.add(site.node());
    }
    for (ImplicitAioobeSite site : cpg.getImplicitAioobeSites()) {
      sites.add(site.node());
    }
    for (ImplicitNegativeArraySizeSite site : cpg.getImplicitNegativeArraySizeSites()) {
      sites.add(site.node());
    }
    for (ImplicitArithmeticSite site : cpg.getImplicitArithmeticSites()) {
      sites.add(site.node());
    }
    return ForwardPathAnalysis.analyseMethodPaths(cpg, sites);
  }

  @Override
  public Optional<ResolvedCallee> resolveCallee(
      final String calleeSignature, final List<SymExpr> actuals) {

    log.debug(
        "resolveReturnExpr: cache present={} inProgress={}",
        summaryRepository.getSummary(calleeSignature).isPresent(),
        summaryRepository.isInProgress(calleeSignature));

    if (summaryRepository.isInProgress(calleeSignature)) {
      return Optional.empty();
    }

    Optional<MethodSummary> cachedSummary = summaryRepository.getSummary(calleeSignature);
    if (cachedSummary.isPresent()) {
      log.debug("summary cache hit for {}", calleeSignature);
      return instantiate(cachedSummary.get(), actuals);
    }

    Optional<WITUpGraph> calleeGraph = graphRepository.getGraph(calleeSignature);
    if (calleeGraph.isEmpty()) {
      log.debug("No graph found for {} — leaving as opaque", calleeSignature);
      return Optional.empty();
    }

    summaryRepository.markInProgress(calleeSignature);
    MethodSummariser calleeAnalysis =
        new MethodSummariser(
            calleeGraph.get(), graphRepository, summaryRepository, emitImplicitExceptions);

    log.debug("summarising callee {}", calleeSignature);
    MethodSummary calleeSummary = calleeAnalysis.summarise();
    summaryRepository.putSummary(calleeSignature, calleeSummary);
    return instantiate(calleeSummary, actuals);
  }

  private Optional<ResolvedCallee> instantiate(
      final MethodSummary summary, final List<SymExpr> actuals) {
    if (!summary.hasReturnExpr()) {
      return Optional.empty();
    }

    InstantiationKey key = new InstantiationKey(summary.methodSignature(), actuals);
    Optional<ResolvedCallee> cachedResolvedCallee = instantiationCache.get(key);
    if (cachedResolvedCallee != null) {
      log.debug("resolved callee cache hit for {}", summary.methodSignature());
      return cachedResolvedCallee;
    }

    List<SymParamRef> formals = summary.formalParams();
    if (formals == null || formals.size() != actuals.size()) {
      log.error("Formal/actual mismatch for {}", summary.methodSignature());
      return Optional.empty();
    }

    List<GuardedExpr> guardedReturn = substituteGuardedReturn(summary, actuals, formals);
    List<List<SymbolicConstraint>> throwPaths =
        substituteThrowConstraints(summary, actuals, formals);

    Optional<ResolvedCallee> resolvedCallee =
        Optional.of(new ResolvedCallee(guardedReturn, throwPaths));
    instantiationCache.put(key, resolvedCallee);
    return resolvedCallee;
  }

  private static List<List<SymbolicConstraint>> substituteThrowConstraints(
      final MethodSummary summary, final List<SymExpr> actuals, final List<SymParamRef> formals) {
    List<List<SymbolicConstraint>> throwPaths = null;
    if (summary.throwConstraints() != null) {
      throwPaths = new ArrayList<>(summary.throwConstraints().size());
      for (List<SymbolicConstraint> path : summary.throwConstraints()) {
        List<SymbolicConstraint> substituted = new ArrayList<>(path.size());
        for (SymbolicConstraint c : path) {
          SymExpr expr = c.symExpr();
          for (int i = 0; i < formals.size(); i++) {
            expr = expr.substituteParam(formals.get(i).getIndex(), actuals.get(i));
          }
          substituted.add(new SymbolicConstraint(expr, c.truthValue()));
        }
        throwPaths.add(substituted);
      }
    }
    return throwPaths;
  }

  private static List<GuardedExpr> substituteGuardedReturn(
      final MethodSummary summary, final List<SymExpr> actuals, final List<SymParamRef> formals) {
    List<GuardedExpr> guardedReturn = new ArrayList<>(summary.guardedReturn().size());
    for (GuardedExpr ge : summary.guardedReturn()) {
      GuardedExpr substituted = ge;
      for (int i = 0; i < formals.size(); i++) {
        substituted = substituted.substituteParam(formals.get(i).getIndex(), actuals.get(i));
      }
      guardedReturn.add(substituted);
    }
    return guardedReturn;
  }
}
