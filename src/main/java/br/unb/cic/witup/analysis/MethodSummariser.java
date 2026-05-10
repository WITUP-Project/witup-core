package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.ImplicitAioobeSite;
import br.unb.cic.witup.analysis.graph.ImplicitArithmeticSite;
import br.unb.cic.witup.analysis.graph.ImplicitNegativeArraySizeSite;
import br.unb.cic.witup.analysis.graph.ImplicitNpeReceiverSite;
import br.unb.cic.witup.analysis.graph.MethodCallSite;
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
      collectCalleePropagatedPaths(exceptionPaths, throwConstraintPaths);
    }

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

      List<List<SymbolicConstraint>> pathConditions =
          symbolicConstraintGenerator.buildThrowConstraintPaths(site.node());
      if (pathConditions.isEmpty()) {
        pathConditions = List.of(List.of());
      }
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

      List<List<SymbolicConstraint>> pathConditions =
          symbolicConstraintGenerator.buildThrowConstraintPaths(site.node());
      if (pathConditions.isEmpty()) {
        pathConditions = List.of(List.of());
      }
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

      List<List<SymbolicConstraint>> pathConditions =
          symbolicConstraintGenerator.buildThrowConstraintPaths(site.node());
      if (pathConditions.isEmpty()) {
        pathConditions = List.of(List.of());
      }
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

      List<List<SymbolicConstraint>> pathConditions =
          symbolicConstraintGenerator.buildThrowConstraintPaths(site.node());
      if (pathConditions.isEmpty()) {
        pathConditions = List.of(List.of());
      }
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

  // For each unguarded call site (no in-scope catch handler) where the callee has a
  // summary, emit one CALLEE_PROPAGATED ExceptionPath per (caller-path × callee-path).
  // Predicate is the caller's path conditions to the call site, conjoined with the
  // callee's predicate after formals→actuals substitution. Provenance prepends the
  // callee's signature to the callee's own provenance chain.
  // Calls in try blocks are deferred to step 3.4 (catch-type matching).
  private void collectCalleePropagatedPaths(
      final List<ExceptionPath> exceptionPaths,
      final List<List<SymbolicConstraint>> throwConstraintPaths) {
    for (MethodCallSite site : cpg.getCallSites()) {
      Set<String> caughtTypes = cpg.inScopeCatchTypes(site.node());
      Optional<MethodSummary> calleeSummaryOpt =
          summaryRepository.getSummary(site.calleeSignature());
      if (calleeSummaryOpt.isEmpty()) {
        // Cache miss: lazy-summarise the callee, mirroring resolveCallee's pattern.
        // Methods with no own throw nodes never trigger resolveCallee during constraint
        // generation, so their callees aren't pre-summarised by that path. Skip if the
        // callee is already in progress (recursive cycle) or has no graph available
        // (e.g. JDK method — opaque, deferred to JDK summaries).
        if (summaryRepository.isInProgress(site.calleeSignature())) {
          continue;
        }
        Optional<WITUpGraph> calleeGraph = graphRepository.getGraph(site.calleeSignature());
        if (calleeGraph.isEmpty()) {
          continue;
        }
        MethodSummariser calleeAnalysis =
            new MethodSummariser(
                calleeGraph.get(), graphRepository, summaryRepository, emitImplicitExceptions);
        calleeSummaryOpt = Optional.of(calleeAnalysis.summarise());
      }
      MethodSummary callee = calleeSummaryOpt.get();
      if (callee.exceptionPaths() == null || callee.exceptionPaths().isEmpty()) {
        continue;
      }
      List<SymParamRef> formals = callee.formalParams();
      if (formals == null || formals.size() != site.actuals().size()) {
        continue;
      }
      List<SymExpr> actuals = new ArrayList<>(site.actuals().size());
      for (var imm : site.actuals()) {
        actuals.add(SymExpr.fromJimple(imm));
      }

      List<List<SymbolicConstraint>> callerPaths =
          symbolicConstraintGenerator.buildThrowConstraintPaths(site.node());
      if (callerPaths.isEmpty()) {
        callerPaths = List.of(List.of());
      }

      for (ExceptionPath calleeEp : callee.exceptionPaths()) {
        if (isCaughtByAny(calleeEp.getExceptionQualifiedName(), caughtTypes)) {
          continue;
        }
        List<SymbolicConstraint> substituted = new ArrayList<>(calleeEp.getConstraints().size());
        for (SymbolicConstraint c : calleeEp.getConstraints()) {
          SymExpr expr = c.symExpr();
          for (int i = 0; i < formals.size(); i++) {
            expr = expr.substituteParam(formals.get(i).getIndex(), actuals.get(i));
          }
          substituted.add(new SymbolicConstraint(expr, c.truthValue()));
        }

        List<String> provenance = new ArrayList<>(calleeEp.getProvenance().size() + 1);
        provenance.add(site.calleeSignature());
        provenance.addAll(calleeEp.getProvenance());

        for (List<SymbolicConstraint> callerPath : callerPaths) {
          List<SymbolicConstraint> combined =
              new ArrayList<>(callerPath.size() + substituted.size());
          combined.addAll(callerPath);
          combined.addAll(substituted);
          exceptionPaths.add(
              new ExceptionPath(
                  combined,
                  site.node(),
                  calleeEp.getExceptionQualifiedName(),
                  ThrowSiteKind.CALLEE_PROPAGATED,
                  provenance));
          throwConstraintPaths.add(combined);
        }
      }
    }
  }

  // Pragmatic catch-type matcher: exact match plus a hardcoded recogniser for the three
  // catch-all aliases at the top of the JDK exception hierarchy. Real subtype matching
  // across user-defined hierarchies is deferred to a SootUp TypeHierarchy integration
  // (see project_deferred_work.md) — this approximation covers the common Commons IO
  // patterns (catch IOException specifically; catch Throwable for defensive absorption).
  private static boolean isCaughtByAny(final String thrownType, final Set<String> caughtTypes) {
    if (thrownType == null || caughtTypes.isEmpty()) {
      return false;
    }
    for (String caught : caughtTypes) {
      if (thrownType.equals(caught)) {
        return true;
      }
      if ("java.lang.Throwable".equals(caught)) {
        return true;
      }
      if ("java.lang.Exception".equals(caught) && !isErrorType(thrownType)) {
        return true;
      }
      if ("java.lang.RuntimeException".equals(caught) && isUncheckedType(thrownType)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isErrorType(final String type) {
    return "java.lang.Error".equals(type)
        || "java.lang.OutOfMemoryError".equals(type)
        || "java.lang.StackOverflowError".equals(type)
        || "java.lang.AssertionError".equals(type);
  }

  private static boolean isUncheckedType(final String type) {
    return "java.lang.RuntimeException".equals(type)
        || "java.lang.NullPointerException".equals(type)
        || "java.lang.IllegalArgumentException".equals(type)
        || "java.lang.IllegalStateException".equals(type)
        || "java.lang.ArrayIndexOutOfBoundsException".equals(type)
        || "java.lang.IndexOutOfBoundsException".equals(type)
        || "java.lang.ArithmeticException".equals(type)
        || "java.lang.NegativeArraySizeException".equals(type)
        || "java.lang.ClassCastException".equals(type)
        || "java.lang.UnsupportedOperationException".equals(type);
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
