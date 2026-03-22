package br.unb.cic.witup.analysis;

// import guru.nidi.graphviz.engine.Format;
// import guru.nidi.graphviz.engine.Graphviz;
// import java.io.File;
import br.unb.cic.witup.analysis.graph.CPGBuilder;
import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public final class ProjectAnalyser implements GraphRepository {
  // assumes jar ia witup-core/project-jars
  private final Path jarPath;
  private static final Logger log = LoggerFactory.getLogger("ProjectAnalyser");
  private final Map<String, WITUpGraph> methodGraphs = new HashMap<>();
  private JavaView view;
  private DefaultDirectedGraph<String, DefaultEdge> callGraph;
  private List<List<String>> analysisOrder;

  public ProjectAnalyser(final Path jarPath) {
    this.jarPath = jarPath;
  }

  public Map<String, WITUpGraph> analyseProject() {
    AnalysisInputLocation inputLocation =
        new JavaClassPathAnalysisInputLocation(jarPath.toAbsolutePath().toString());
    view = new JavaView(inputLocation);

    List<JavaSootClass> classes = view.getClasses().toList();

    Map<String, WITUpGraph> methodGraph = analyseClasses(classes);
    methodGraphs.putAll(methodGraph);
    return methodGraphs;
  }

  private Map<String, WITUpGraph> analyseClasses(final List<JavaSootClass> classes) {
    return classes.stream()
        .map(this::analyseClass)
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, WITUpGraph> analyseClass(final JavaSootClass sootClass) {
    return buildGraphsForClass(sootClass);
  }

  public AnalysisResult analyseMethod(final String methodSignature) {
    WITUpGraph graph = methodGraphs.get(methodSignature);
    if (graph == null) {
      throw new IllegalArgumentException("No graph for: " + methodSignature);
    }

    if (callGraph == null) {
      CallGraphBuilder builder = new CallGraphBuilder(view);
      callGraph = builder.build(methodGraphs.keySet());
      analysisOrder = builder.buildAnalysisOrder(callGraph);
    }

    Set<String> reachable = findReachable(methodSignature);
    SummaryCache summaryCache = new SummaryCache();
    Map<String, String> failures = new LinkedHashMap<>();
    Map<String, MethodSummary> localSummaries = new LinkedHashMap<>();

    // build topological order for just this method and its transitive callees
    for (List<String> scc : analysisOrder) {
      List<String> relevant = scc.stream().filter(reachable::contains).collect(Collectors.toList());
      if (relevant.isEmpty()) {
        continue;
      }
      if (relevant.size() == 1) {
        summariseMethod(relevant.getFirst(), methodGraphs, summaryCache, localSummaries, failures);
      } else {
        summariseSCC(relevant, methodGraphs, summaryCache, localSummaries, failures);
      }
    }

    MethodSummary summary = localSummaries.get(methodSignature);
    SymbolicConstraintSolver solver =
        new SymbolicConstraintSolver(Map.of(methodSignature, summary));
    Map<String, List<SolverResult>> solutions = solver.solveConstraintsSafe(failures);

    return new AnalysisResult(graph, summary, solutions.get(methodSignature), failures);
  }

  private Set<String> findReachable(final String root) {
    Set<String> reachable = new HashSet<>();
    Deque<String> worklist = new ArrayDeque<>();
    worklist.add(root);

    String className = root.substring(1, root.indexOf(':'));

    methodGraphs.keySet().stream()
        .filter(sig -> sig.startsWith("<" + className + ": ") && sig.contains("lambda$"))
        .forEach(reachable::add);

    while (!worklist.isEmpty()) {
      String sig = worklist.pop();
      if (!reachable.add(sig)) {
        continue;
      }
      if (!callGraph.containsVertex(sig)) {
        continue;
      }
      callGraph
          .outgoingEdgesOf(sig)
          .forEach(
              e -> {
                String callee = callGraph.getEdgeTarget(e);
                if (methodGraphs.containsKey(callee)) {
                  worklist.add(callee);
                }
              });
    }
    return reachable;
  }

  public static Map<String, WITUpGraph> buildGraphsForClass(final JavaSootClass sootClass) {
    return sootClass.getMethods().stream()
        .filter(JavaSootMethod::hasBody)
        .collect(Collectors.toMap(m -> m.getSignature().toString(), CPGBuilder::buildForMethod));
  }

  @Override
  public Optional<WITUpGraph> getGraph(final String methodSignature) {
    return Optional.ofNullable(methodGraphs.get(methodSignature));
  }

  public Map<String, MethodSummary> summariseAll(
      final Map<String, WITUpGraph> graphs, final Map<String, String> failures) {

    SummaryCache summaryCache = new SummaryCache();
    Map<String, MethodSummary> methodSummaries = new LinkedHashMap<>();

    CallGraphBuilder builder = new CallGraphBuilder(view);
    callGraph = builder.build(graphs.keySet());
    analysisOrder = builder.buildAnalysisOrder(callGraph);
    for (List<String> scc : analysisOrder) {
      if (scc.size() == 1) {
        // singleton — one pass
        String sig = scc.getFirst();
        summariseMethod(sig, graphs, summaryCache, methodSummaries, failures);
      } else {
        summariseSCC(scc, graphs, summaryCache, methodSummaries, failures);
      }
    }
    return methodSummaries;
  }

  private void summariseMethod(
      final String sig,
      final Map<String, WITUpGraph> graphs,
      final SummaryCache summaryCache,
      final Map<String, MethodSummary> summaries,
      final Map<String, String> failures) {
    WITUpGraph graph = graphs.get(sig);
    if (graph == null) {
      return;
    }
    try {
      MethodSummariser ms = new MethodSummariser(graph, this, summaryCache);
      summaries.put(sig, ms.summarise());
    } catch (Exception e) {
      log.warn("Failed to summarise {}: {}", sig, e.getMessage());
      failures.put(sig, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
  }

  /** We land here when there is mutual or direct recursion. */
  private void summariseSCC(
      final List<String> scc,
      final Map<String, WITUpGraph> graphs,
      final SummaryCache summaryCache,
      final Map<String, MethodSummary> summaries,
      final Map<String, String> failures) {
    // initialise all methods in SCC with empty summaries
    for (String sig : scc) {
      WITUpGraph graph = graphs.get(sig);
      if (graph == null) {
        continue;
      }
      summaryCache.putSummary(sig, MethodSummary.empty(sig));
    }
    // iterate until fixpoint
    boolean changed = true;
    int iterations = 0;
    while (changed) {
      changed = false;
      iterations++;
      for (String sig : scc) {
        WITUpGraph graph = graphs.get(sig);
        if (graph == null) {
          continue;
        }
        try {
          MethodSummariser ms = new MethodSummariser(graph, this, summaryCache);
          MethodSummary newSummary = ms.summarise();
          MethodSummary oldSummary = summaryCache.getSummary(sig).orElse(null);
          if (!newSummary.equals(oldSummary)) {
            summaryCache.putSummary(sig, newSummary);
            changed = true;
          }
        } catch (Exception e) {
          failures.put(sig, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
      }
      log.debug("SCC fixpoint iteration {} — changed={}", iterations, changed);
    }
    // "persist"
    for (String sig : scc) {
      summaryCache.getSummary(sig).ifPresent(s -> summaries.put(sig, s));
    }
  }

  //  private void dumpGraph(final WITUpGraph graph, final String sig) {
  //    try {
  //      String safeName = sig.replaceAll("[<>:()\\s,]", "_");
  //      File graphsDir = new File("graphs/failures");
  //      if (!graphsDir.exists()) {
  //        graphsDir.mkdirs();
  //      }
  //      Graphviz.fromString(graph.getDot())
  //              .render(Format.SVG)
  //              .toFile(new File(graphsDir, safeName + ".svg"));
  //      log.info("Dumped failure graph for {}", sig);
  //    } catch (Exception e) {
  //      log.warn("Could not dump graph for {}: {}", sig, e.getMessage());
  //    }
  //  }
}
