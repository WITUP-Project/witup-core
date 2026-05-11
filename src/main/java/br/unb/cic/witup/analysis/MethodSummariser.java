package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.ImplicitAioobeSite;
import br.unb.cic.witup.analysis.graph.ImplicitArithmeticSite;
import br.unb.cic.witup.analysis.graph.ImplicitNegativeArraySizeSite;
import br.unb.cic.witup.analysis.graph.ImplicitNpeReceiverSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  /**
   * Interprocedural MethodSummariser
   *
   * @param cpg WITUpGraph of the method being analysed
   * @param graphRepository GraphRepository
   * @param summaryRepository SummaryRepository
   * @param emitImplicitExceptions when true, synthesise rollup-level ExceptionPaths beyond
   *     the method's own athrow sites: IMPLICIT-kind paths for JVM implicit exceptions (NPE,
   *     AIOOBE, NegativeArraySize, ArithmeticException) and CALLEE_PROPAGATED-kind paths for
   *     uncaught escapes from callees. Both classes are gated together. Legacy tests remain
   *     on the default-off branch for now — they'll be revisited so they reflect the new
   *     synthesis when we trust it.
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
      String exceptionQualifiedName = cpg.resolveExceptionType(throwStmt);
      ThrowSiteKind throwSiteKind = cpg.classifyThrowSite(throwStmt);
      for (List<SymbolicConstraint> constraints :
          symbolicConstraintGenerator.buildThrowConstraintPaths(throwNode)) {
        exceptionPaths.add(
            new ExceptionPath(
                constraints, throwNode, exceptionQualifiedName, throwSiteKind, List.of()));
        throwConstraintPaths.add(constraints);
      }
    }

    if (emitImplicitExceptions) {
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

  // For each instance-method invocation in the body, synthesise an ExceptionPath whose
  // predicate conjoins the path conditions reaching the call site with `receiver == null`.
  // Empty path-condition lists (sites unconditionally reachable from entry) fall back to
  // a single empty list so the null check still emits.
  private void collectImplicitNpePaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitNpeReceiverSite site : cpg.getImplicitNpeReceiverSites()) {
      SymExpr receiverExpr = SymExpr.fromJimple(site.receiver());
      SymbolicConstraint nullCheck =
          new SymbolicConstraint(new SymBinOp(BinOp.EQ, receiverExpr, SymNull.INSTANCE), true);

      // Caller branch context dropped — see deferred work for restoration plan. Per-site
      // backward substitution on Commons IO blew the heap; recall is preserved (the rollup
      // predicate stands on its own), only branch-gated precision is lost.
      List<List<SymbolicConstraint>> pathConditions = List.of(List.of());
      for (List<SymbolicConstraint> path : pathConditions) {
        List<SymbolicConstraint> withNull = new ArrayList<>(path.size() + 1);
        withNull.addAll(path);
        withNull.add(nullCheck);
        exceptionPaths.add(
            new ExceptionPath(
                withNull,
                site.node(),
                "java.lang.NullPointerException",
                ThrowSiteKind.IMPLICIT,
                List.of()));
        throwConstraintPaths.add(withNull);
      }
    }
  }

  // For each array element access, synthesise an ExceptionPath whose predicate conjoins
  // the path conditions reaching the access with `arr != null AND (i < 0 || i >= arr.length)`.
  // The non-null conjunct distinguishes the AIOOBE witness from the NPE-on-array-base case
  // (the JVM raises NPE before AIOOBE when both could fire). Empty path-condition lists
  // fall back to a single empty list so the bounds check still emits.
  private void collectImplicitAioobePaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitAioobeSite site : cpg.getImplicitAioobeSites()) {
      SymExpr arrExpr = SymExpr.fromJimple(site.arrayBase());
      SymExpr indexExpr = SymExpr.fromJimple(site.index());
      SymExpr nonNull = new SymBinOp(BinOp.NE, arrExpr, SymNull.INSTANCE);
      SymExpr lt = new SymBinOp(BinOp.LT, indexExpr, SymIntConst.zero());
      SymExpr ge = new SymBinOp(BinOp.GE, indexExpr, new SymLength(arrExpr));
      SymExpr orBounds = new SymBinOp(BinOp.OR, lt, ge);
      SymbolicConstraint boundsCheck =
          new SymbolicConstraint(new SymBinOp(BinOp.AND, nonNull, orBounds), true);

      // Caller branch context dropped — see deferred work for restoration plan. Per-site
      // backward substitution on Commons IO blew the heap; recall is preserved (the rollup
      // predicate stands on its own), only branch-gated precision is lost.
      List<List<SymbolicConstraint>> pathConditions = List.of(List.of());
      for (List<SymbolicConstraint> path : pathConditions) {
        List<SymbolicConstraint> withBounds = new ArrayList<>(path.size() + 1);
        withBounds.addAll(path);
        withBounds.add(boundsCheck);
        exceptionPaths.add(
            new ExceptionPath(
                withBounds,
                site.node(),
                "java.lang.ArrayIndexOutOfBoundsException",
                ThrowSiteKind.IMPLICIT,
                List.of()));
        throwConstraintPaths.add(withBounds);
      }
    }
  }

  // For each `new T[n]` allocation, synthesise an ExceptionPath whose predicate conjoins
  // the path conditions reaching the allocation with `size < 0`.
  private void collectImplicitNegativeArraySizePaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitNegativeArraySizeSite site : cpg.getImplicitNegativeArraySizeSites()) {
      SymExpr sizeExpr = SymExpr.fromJimple(site.size());
      SymbolicConstraint negativeCheck =
          new SymbolicConstraint(new SymBinOp(BinOp.LT, sizeExpr, SymIntConst.zero()), true);

      // Caller branch context dropped — see deferred work for restoration plan. Per-site
      // backward substitution on Commons IO blew the heap; recall is preserved (the rollup
      // predicate stands on its own), only branch-gated precision is lost.
      List<List<SymbolicConstraint>> pathConditions = List.of(List.of());
      for (List<SymbolicConstraint> path : pathConditions) {
        List<SymbolicConstraint> withNegative = new ArrayList<>(path.size() + 1);
        withNegative.addAll(path);
        withNegative.add(negativeCheck);
        exceptionPaths.add(
            new ExceptionPath(
                withNegative,
                site.node(),
                "java.lang.NegativeArraySizeException",
                ThrowSiteKind.IMPLICIT,
                List.of()));
        throwConstraintPaths.add(withNegative);
      }
    }
  }

  // For each integer `/` or `%`, synthesise an ExceptionPath whose predicate conjoins the
  // path conditions reaching the operation with `divisor == 0`.
  private void collectImplicitArithmeticPaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (ImplicitArithmeticSite site : cpg.getImplicitArithmeticSites()) {
      SymExpr divisorExpr = SymExpr.fromJimple(site.divisor());
      SymbolicConstraint zeroCheck =
          new SymbolicConstraint(new SymBinOp(BinOp.EQ, divisorExpr, SymIntConst.zero()), true);

      // Caller branch context dropped — see deferred work for restoration plan. Per-site
      // backward substitution on Commons IO blew the heap; recall is preserved (the rollup
      // predicate stands on its own), only branch-gated precision is lost.
      List<List<SymbolicConstraint>> pathConditions = List.of(List.of());
      for (List<SymbolicConstraint> path : pathConditions) {
        List<SymbolicConstraint> withZero = new ArrayList<>(path.size() + 1);
        withZero.addAll(path);
        withZero.add(zeroCheck);
        exceptionPaths.add(
            new ExceptionPath(
                withZero,
                site.node(),
                "java.lang.ArithmeticException",
                ThrowSiteKind.IMPLICIT,
                List.of()));
        throwConstraintPaths.add(withZero);
      }
    }
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
