package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BinOp;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymExpr;
import br.unb.cic.witup.analysis.symbolic.SymITE;
import br.unb.cic.witup.analysis.symbolic.SymIntConst;
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

  /**
   * Intraproccedural summariser. Has no access to cached graphs or summaries
   *
   * @param cpg WITUpGraph
   */
  public MethodSummariser(final WITUpGraph cpg) {
    this(cpg, null, null);
  }

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
  }

  /**
   * Recursively produces MethodSummary.
   *
   */
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
    log.debug("traceReturnExpr called for {}", getMethodSignature());
    List<ReturnStatementNode> returnNodes = cpg.getReturnNodes();
    if (returnNodes.isEmpty()) {
      log.debug("No return nodes found for {}", getMethodSignature());
      return null;
    }

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, List.of(), null);

    SymExpr result = null;

    for (ReturnStatementNode returnNode : returnNodes) {
      SymExpr returnExpr = sg.generateReturnExpression(returnNode);
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

    log.debug("traceReturnExpr for {} — {}", getMethodSignature(), result);
    return result;
  }

  private SymExpr buildPathCondition(final ReturnStatementNode returnNode) {
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      return SymIntConst.one();
    }

    // build a condition per path, then disjoin them
    // ITE(cond1, 1, ITE(cond2, 1, ITE(cond3, 1, 0)))
    SymExpr result = SymIntConst.zero();

    for (int p = paths.size() - 1; p >= 0; p--) {
      List<List<SymbolicConstraint>> generated =
              new SymbolicConstraintGenerator(cpg, List.of(paths.get(p)), null)
                      .generateSymbolicConstraintPaths();

      if (generated.isEmpty() || generated.get(0).isEmpty()) {
        return SymIntConst.one(); // unconditional path exists — always reachable
      }

      List<SymbolicConstraint> constraints = generated.get(0);
      SymExpr pathCond = SymIntConst.one();
      for (int i = constraints.size() - 1; i >= 0; i--) {
        SymbolicConstraint c = constraints.get(i);
        SymExpr cond = c.getTruthValue()
                ? c.getSymExpr()
                : new SymBinOp(BinOp.EQ, c.getSymExpr(), SymIntConst.zero());
        pathCond = new SymITE(cond, pathCond, SymIntConst.zero());
      }

      // disjoin: if this path's condition holds, result is 1
      result = new SymITE(pathCond, SymIntConst.one(), result);
    }

    return result;
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

    log.debug("Calling summarise() for callee {}", calleeSignature);
    MethodSummary calleeSummary = calleeAnalysis.summarise();
    log.debug(
        "summarise() returned for {} — returnExpr={}",
        calleeSignature,
        calleeSummary.getReturnExpr());
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

    log.debug("instantiate: formals={} actuals={}", summary.getFormalParams(), actuals);


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
