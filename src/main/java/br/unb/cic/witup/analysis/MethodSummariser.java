package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymITE;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
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
  private static final Logger log = LoggerFactory.getLogger("MethodSummariser");
  public static final int MAX_THROW_FREE_PATHS = 10000;

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
            .flatMap(throwNode -> buildSymbolicConstraintPaths(throwNode).stream())
            .collect(Collectors.toList());

    List<SymParamRef> formals = buildFormals(cpg);
    SymExpr returnExpr = traceReturnExpr();
    SymExpr throwFreePrecondition = buildThrowFreePrecondition(paths);
    MethodSummary summary =
        new MethodSummary(getMethodSignature(), paths, formals, returnExpr, throwFreePrecondition);

    if (summaryRepository != null) {
      summaryRepository.putSummary(sig, summary);
    }

    return summary;
  }

  private SymExpr buildThrowFreePrecondition(final List<List<SymbolicConstraint>> paths) {
    if (paths == null || paths.isEmpty()) {
      return null;
    }
    List<List<SymbolicConstraint>> boundedThrowFreePaths =
        paths.size() > MAX_THROW_FREE_PATHS ? paths.subList(0, MAX_THROW_FREE_PATHS) : paths;
    // for each path, build the negation of its conjunction
    // throw-free means: NOT(path1) AND NOT(path2) AND ...
    // NOT(path) = NOT(c1 AND c2 AND ...) = NOT(c1) OR NOT(c2) OR ...
    // but for simplicity encode as ITE tree (may cost a lot of memory)
    // throw-free precondition: all throw paths are false
    // encode as: ITE(throwCond1, 0, ITE(throwCond2, 0, 1))
    SymExpr result = SymIntConst.one();
    for (List<SymbolicConstraint> path : boundedThrowFreePaths) {
      SymExpr pathCond = generatePathConditions(path);
      result = new SymITE(pathCond, SymIntConst.zero(), result);
    }
    return result;
  }

  public static SymExpr generatePathConditions(final List<SymbolicConstraint> constraints) {
    SymExpr result = SymIntConst.one();
    // traverse constraints backwards to build the recursive ITE
    for (int i = constraints.size() - 1; i >= 0; i--) {
      SymbolicConstraint c = constraints.get(i);
      SymExpr cond =
          c.truthValue()
              ? c.symExpr()
              : new SymBinOp(BinOp.EQ, c.symExpr(), SymIntConst.zero());
      result = new SymITE(cond, result, SymIntConst.zero());
    }
    return result;
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
    // @this in -1 index
    if (!cpg.getMethod().isStatic()) {
      formals.add(new SymParamRef(-1, cpg.getMethod().getDeclaringClassType()));
    }
    return formals;
  }

  private SymExpr traceReturnExpr() {
    List<ReturnStatementNode> returnNodes = cpg.getReturnNodes();
    if (returnNodes.isEmpty()) {
      return null;
    }

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, List.of(), this);

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
          new SymbolicConstraintGenerator(cpg, List.of(paths.get(p)), this)
              .generateSymbolicConstraintPaths();

      if (generated.isEmpty() || generated.getFirst().isEmpty()) {
        return SymIntConst.one(); // unconditional path exists — always reachable
      }
      List<SymbolicConstraint> constraints = generated.getFirst();
      SymExpr pathCond = generatePathConditions(constraints);
      // disjoin: if this path's condition holds, result is 1
      result = new SymITE(pathCond, SymIntConst.one(), result);
    }

    return result;
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
