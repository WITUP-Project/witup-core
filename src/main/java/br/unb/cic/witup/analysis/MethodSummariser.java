package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraintGenerator;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
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

  /**
   * Interprocedural MethodSummariser. As of now,
   *
   * @param cpg WITUpGraph of the method being analysed
   * @param graphRepository GraphRepository
   * @param summaryRepository SummaryRepository
   */
  public MethodSummariser(
      final WITUpGraph cpg,
      final GraphRepository graphRepository,
      final SummaryRepository summaryRepository) {
    this.cpg = cpg;
    this.graphRepository = graphRepository;
    this.summaryRepository = summaryRepository;
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
    List<List<SymbolicConstraint>> paths = new ArrayList<>();
    for (WITUpNode throwNode : cpg.getThrowNodes()) {
      String exceptionQualifiedName = cpg.resolveExceptionType((ThrowStatementNode) throwNode);
      for (List<SymbolicConstraint> constraints :
          symbolicConstraintGenerator.buildSymbolicConstraintPaths(throwNode)) {
        exceptionPaths.add(new ExceptionPath(constraints, throwNode, exceptionQualifiedName));
        paths.add(constraints);
      }
    }

    List<SymParamRef> formals = symbolicConstraintGenerator.buildFormals();
    List<GuardedExpr> guardedReturn = symbolicConstraintGenerator.traceReturnGuarded();
    MethodSummary summary =
        new MethodSummary(cpg.getMethodSignature(), exceptionPaths, formals, guardedReturn, paths);

    summaryRepository.putSummary(sig, summary);
    return summary;
  }

  @Override
  public Optional<ResolvedCallee> resolveReturnExpr(
      final String calleeSignature, final List<SymExpr> actuals) {

    if (summaryRepository == null || graphRepository == null) {
      return Optional.empty();
    }

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
        new MethodSummariser(calleeGraph.get(), graphRepository, summaryRepository);

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

    List<GuardedExpr> guardedReturn = new ArrayList<>(summary.guardedReturn().size());
    for (GuardedExpr ge : summary.guardedReturn()) {
      GuardedExpr substituted = ge;
      for (int i = 0; i < formals.size(); i++) {
        substituted = substituted.substituteParam(formals.get(i).getIndex(), actuals.get(i));
      }
      guardedReturn.add(substituted);
    }

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

    var result = Optional.of(new ResolvedCallee(guardedReturn, throwPaths));
    instantiationCache.put(key, result);
    return result;
  }
}
