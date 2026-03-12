package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymExpr;
import br.unb.cic.witup.analysis.symbolic.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.types.Type;

/**
 * Given a method that throws, build the symbolic constraints for each path leading to throw nodes.
 */
public final class MethodConstraintAnalysis implements SummaryResolver {
  private static final Logger log = LoggerFactory.getLogger("MethodConstraintAnalysis");

  private final WITUpGraph cpg;
  private final Map<WITUpNode, List<List<SymbolicConstraint>>> symbolicThrowConstraints =
      new HashMap<>();
  private final GraphRepository graphRepository;
  private final SummaryRepository summaryRepository;

  public MethodConstraintAnalysis(final WITUpGraph cpg) {
    this(cpg, null, null);
  }

  public MethodConstraintAnalysis(
      final WITUpGraph cpg,
      final GraphRepository graphRepository,
      final SummaryRepository summaryRepository) {
    this.cpg = cpg;
    this.graphRepository = graphRepository;
    this.summaryRepository = summaryRepository;
  }

  public static Map<String, MethodSummary> summariseAll(
      final Map<String, WITUpGraph> methodGraphs) {
    return methodGraphs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    new MethodConstraintAnalysis(entry.getValue()).summariseConstraintPaths()));
  }

  @Override
  public Optional<SymExpr> resolveReturnExpr(
      final String calleeSignature, final List<SymExpr> actuals) {
    if (summaryRepository == null || graphRepository == null) {
      log.debug("Interprocedural resolution skipped — no repository for {}", calleeSignature);
      return Optional.empty();
    }

    // any alternatives to being conservatives here or mathematically impossible?
    if (summaryRepository.isInProgress(calleeSignature)) {
      log.debug("Recursive call detected for {} — returning conservative empty", calleeSignature);
      return Optional.empty();
    }

    Optional<MethodSummary> cachedSummary = summaryRepository.getSummary(calleeSignature);
    if (cachedSummary.isPresent()) {
      log.debug("Cache hit for {}", calleeSignature);
      // instantiate returnExpr with actuals when MethodSummary carries returnExpr
      return Optional.empty();
    }

    Optional<WITUpGraph> calleeGraph = graphRepository.getGraph(calleeSignature);
    if (calleeGraph.isEmpty()) {
      log.debug("No graph found for {} — leaving as opaque", calleeSignature);
      return Optional.empty();
    }

    log.debug("Recursively analysing callee {}", calleeSignature);
    summaryRepository.markInProgress(calleeSignature);
    MethodConstraintAnalysis calleeAnalysis =
        new MethodConstraintAnalysis(calleeGraph.get(), graphRepository, summaryRepository);
    MethodSummary calleeSummary = calleeAnalysis.summariseConstraintPaths();
    summaryRepository.putSummary(calleeSignature, calleeSummary);

    // instantiate returnExpr with actuals when MethodSummary carries returnExpr
    log.debug(
        "Callee {} analysed — return expr instantiation not yet implemented", calleeSignature);
    return Optional.empty();
  }

  public List<List<SymbolicConstraint>> getSymbolicConstraintPaths(final WITUpNode throwNode) {
    return symbolicThrowConstraints.computeIfAbsent(
        throwNode,
        node -> {
          var constraintPaths = cpg.getConstraintPaths(node);
          BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);
          return sg.generateSymbolicConstraintPaths();
        });
  }

  public String getMethodSignature() {
    return cpg.getMethodSignature();
  }

  public List<WITUpNode> getThrowNodes() {
    return cpg.getThrowNodes();
  }

  public MethodSummary summariseConstraintPaths() {
    String sig = getMethodSignature();

    if (summaryRepository != null) {
      Optional<MethodSummary> cached = summaryRepository.getSummary(sig);
      if (cached.isPresent()) {
        return cached.get();
      }
      summaryRepository.markInProgress(sig);
    }

    List<List<SymbolicConstraint>> paths =
        getThrowNodes().stream()
            .flatMap(node -> getSymbolicConstraintPaths(node).stream())
            .collect(Collectors.toList());

    List<SymParamRef> formals = buildFormals(cpg);
    MethodSummary summary = new MethodSummary(getMethodSignature(), paths, formals, null);

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
    return formals;
  }
}
