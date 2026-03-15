package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymExpr;
import br.unb.cic.witup.analysis.symbolic.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraintGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.types.Type;

/**
 * Given a method that throws, build the symbolic constraints for each path leading to throw nodes.
 */
public final class MethodSummariser implements SummaryResolver {
  private static final Logger log = LoggerFactory.getLogger("MethodConstraintAnalysis");

  private final WITUpGraph cpg;
  private final Map<WITUpNode, List<List<SymbolicConstraint>>> symbolicThrowConstraints =
      new HashMap<>();
  private final GraphRepository graphRepository;
  private final SummaryRepository summaryRepository;

  public MethodSummariser(final WITUpGraph cpg) {
    this(cpg, null, null);
  }

  public MethodSummariser(
      final WITUpGraph cpg,
      final GraphRepository graphRepository,
      final SummaryRepository summaryRepository) {
    this.cpg = cpg;
    this.graphRepository = graphRepository;
    this.summaryRepository = summaryRepository;
  }

  public MethodSummary summarise() {
    String sig = getMethodSignature();

    if (summaryRepository != null) {
      Optional<MethodSummary> cached = summaryRepository.getSummary(sig);
      if (cached.isPresent()) {
        return cached.get();
      }
      summaryRepository.markInProgress(sig);
    }

    List<List<SymbolicConstraint>> paths =
        cpg.getThrowNodes().stream()
            .flatMap(node -> buildSymbolicConstraintPaths(node).stream())
            .collect(Collectors.toList());

    List<SymParamRef> formals = buildFormals(cpg);
    SymExpr returnExpr = traceReturnExpr();

    MethodSummary summary = new MethodSummary(getMethodSignature(), paths, formals, returnExpr);

    if (summaryRepository != null) {
      summaryRepository.putSummary(sig, summary);
    }

    return summary;
  }

  public List<List<SymbolicConstraint>> buildSymbolicConstraintPaths(final WITUpNode throwNode) {
    return symbolicThrowConstraints.computeIfAbsent(
        throwNode,
        node -> {
          var constraintPaths = cpg.getConstraintPaths(node);
          SymbolicConstraintGenerator sg =
              new SymbolicConstraintGenerator(cpg, constraintPaths, this);
          return sg.generateSymbolicConstraintPaths();
        });
  }

  private static List<SymParamRef> buildFormals(final WITUpGraph cpg) {
    List<Type> paramTypes = cpg.getMethod().getParameterTypes();
    List<SymParamRef> formals = new ArrayList<>();
    for (int i = 0; i < paramTypes.size(); i++) {
      formals.add(new SymParamRef(i, paramTypes.get(i)));
    }
    return formals;
  }

  private SymExpr traceReturnExpr() {
    List<ReturnStatementNode> returnNodes = cpg.getReturnNodes();
    if (returnNodes.isEmpty()) {
      log.debug("No return nodes found for {}", getMethodSignature());
      return null;
    }
    if (returnNodes.size() > 1) {
      // encode multiple return nodes as Z3 If-Then-Else
      log.debug("Multiple return nodes for {} — using first", getMethodSignature());
    }
    ReturnStatementNode returnNode = returnNodes.get(0);
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getConstraintPaths(returnNode);
    log.debug("traceReturnExpr for {} returnNode op: {}", getMethodSignature(), returnNode.getOp());
    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, paths, this);
    return sg.generateReturnExpression(returnNode);
  }

  @Override
  public Optional<SymExpr> resolveReturnExpr(
      final String calleeSignature, final List<SymExpr> actuals) {
    log.debug("resolveReturnExpr called for: {}", calleeSignature);
    log.debug("summaryRepository null: {}", summaryRepository == null);
    log.debug("graphRepository null: {}", graphRepository == null);
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
      return instantiate(cachedSummary.get(), actuals);
    }

    Optional<WITUpGraph> calleeGraph = graphRepository.getGraph(calleeSignature);
    if (calleeGraph.isEmpty()) {
      log.debug("No graph found for {} — leaving as opaque", calleeSignature);
      return Optional.empty();
    }

    log.debug("Recursively analysing callee {}", calleeSignature);
    summaryRepository.markInProgress(calleeSignature);
    MethodSummariser calleeAnalysis =
        new MethodSummariser(calleeGraph.get(), graphRepository, summaryRepository);
    MethodSummary calleeSummary = calleeAnalysis.summarise();
    summaryRepository.putSummary(calleeSignature, calleeSummary);

    // instantiate returnExpr with actuals when MethodSummary carries returnExpr
    log.debug(
        "Callee {} analysed — return expr instantiation not yet implemented", calleeSignature);
    return instantiate(calleeSummary, actuals);
  }

  private Optional<SymExpr> instantiate(final MethodSummary summary, final List<SymExpr> actuals) {
    if (!summary.hasReturnExpr()) {
      log.debug("No return expr in summary for {}", summary.getMethodSignature());
      return Optional.empty();
    }

    List<SymParamRef> formals = summary.getFormalParams();
    if (formals == null || formals.size() != actuals.size()) {
      log.debug("Formal/actual mismatch for {}", summary.getMethodSignature());
      return Optional.empty();
    }

    SymExpr returnExpr = summary.getReturnExpr();
    for (int i = 0; i < formals.size(); i++) {
      returnExpr = returnExpr.substituteParam(formals.get(i).getIndex(), actuals.get(i));
    }
    log.debug("Instantiated return expr for {}: {}", summary.getMethodSignature(), returnExpr);
    return Optional.of(returnExpr);
  }

  public String getMethodSignature() {
    return cpg.getMethodSignature();
  }
}
