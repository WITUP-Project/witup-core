package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraintGenerator;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymITE;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.types.Type;

/**
 * Given a method that throws, build the symbolic constraints for each path leading to throw nodes.
 */
public final class MethodSummariser implements SummaryResolver {
  private static final Logger log = LoggerFactory.getLogger("MethodSummariser");

  private final WITUpGraph cpg;
  private final GraphRepository graphRepository;
  private final SummaryRepository summaryRepository;
  private final SymbolicConstraintGenerator symbolicConstraintGenerator;

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
    String sig = getMethodSignature();

    // this should never be null so consider ensuring that upstream
    // code will always set it
    if (summaryRepository != null) {
      Optional<MethodSummary> cached = summaryRepository.getSummary(sig);
      if (cached.isPresent()) {
        return cached.get();
      }
      summaryRepository.markInProgress(sig);
    }

    List<List<SymbolicConstraint>> paths =
        cpg.getThrowNodes().stream()
            .flatMap(
                throwNode ->
                    symbolicConstraintGenerator.buildSymbolicConstraintPaths(throwNode).stream())
            .collect(Collectors.toList());

    List<SymParamRef> formals = buildFormals(cpg);
    SymExpr returnExpr = symbolicConstraintGenerator.traceReturnExpr();
    SymExpr throwFreePrecondition = symbolicConstraintGenerator.buildThrowFreePrecondition(paths);
    MethodSummary summary =
        new MethodSummary(getMethodSignature(), paths, formals, returnExpr, throwFreePrecondition);

    if (summaryRepository != null) {
      summaryRepository.putSummary(sig, summary);
    }

    return summary;
  }

  private static List<SymParamRef> buildFormals(final WITUpGraph cpg) {
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

  public record ResolvedCallee(SymExpr returnExpr, SymExpr precondition) {}

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

    // any alternatives to being conservatives here or mathematically impossible?
    if (summaryRepository.isInProgress(calleeSignature)) {
      return Optional.empty();
    }

    Optional<MethodSummary> cachedSummary = summaryRepository.getSummary(calleeSignature);
    if (cachedSummary.isPresent()) {
      log.debug("Cache hit for {}", calleeSignature);
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

    List<SymParamRef> formals = summary.getFormalParams();
    if (formals == null || formals.size() != actuals.size()) {
      log.error("Formal/actual mismatch for {}", summary.getMethodSignature());
      return Optional.empty();
    }

    SymExpr returnExpr = summary.getReturnExpr();
    SymExpr precondition = summary.getThrowFreePrecondition();

    for (int i = 0; i < formals.size(); i++) {
      int idx = formals.get(i).getIndex();
      SymExpr actual = actuals.get(i);
      returnExpr = returnExpr.substituteParam(idx, actual);
      returnExpr = returnExpr.substituteParam(idx, actual);
      if (precondition != null) {
        precondition = precondition.substituteParam(idx, actual);
      }
    }
    return Optional.of(new ResolvedCallee(returnExpr, precondition));
  }

  public String getMethodSignature() {
    return cpg.getMethodSignature();
  }
}
