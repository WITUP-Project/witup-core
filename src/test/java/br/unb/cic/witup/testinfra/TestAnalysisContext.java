package br.unb.cic.witup.testinfra;

import br.unb.cic.witup.analysis.ExceptionFlowWalker;
import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyses all of our sample classes and makes them available to our tests. Fine if we are running
 * on a single thread. Probably should scope this to class level
 */
public final class TestAnalysisContext {
  private static final Map<String, WITUpGraph> graphs;
  private static final Map<String, MethodSummary> summaries;
  private static final Map<String, List<SolverResult>> solutions;
  private static final Map<String, String> failures;
  private static final ProjectAnalyser projectAnalyser;
  private static ProjectAnalyser implicitProjectAnalyser;
  private static Map<String, WITUpGraph> implicitGraphs;
  private static Map<String, MethodSummary> implicitSummaries;
  private static ExceptionFlowWalker implicitWalker;

  static {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");

    projectAnalyser = new ProjectAnalyser(testClassesDir);
    graphs = projectAnalyser.analyseProject();

    failures = new LinkedHashMap<>();
    summaries = new LinkedHashMap<>();
    solutions = new LinkedHashMap<>();
  }

  public static Map<String, WITUpGraph> getGraphs() {
    return graphs;
  }

  public static Map<String, MethodSummary> getSummaries() {
    return summaries;
  }

  public static Map<String, List<SolverResult>> getSolutions() {
    return solutions;
  }

  public static Map<String, String> getFailures() {
    return failures;
  }

  public static ProjectAnalyser getAnalyser() {
    return projectAnalyser;
  }

  public static ProjectAnalyser getImplicitAnalyser() {
    if (implicitProjectAnalyser == null) {
      Path testClassesDir =
          Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
      ProjectAnalyser local = new ProjectAnalyser(testClassesDir, true);
      implicitGraphs = local.analyseProject();
      implicitProjectAnalyser = local;
    }
    return implicitProjectAnalyser;
  }

  /**
   * Walker over the implicit-exceptions analyser, so tests can see the composed observable view.
   * CALLEE_PROPAGATED paths are not stored in a MethodSummary. provenance is only ever populated
   * here. A test that goes through analyseMethod().summary() structurally cannot see a
   * cross-provenance duplicate.
   */
  public static ExceptionFlowWalker getImplicitWalker() {
    if (implicitWalker == null) {
      ProjectAnalyser analyser = getImplicitAnalyser();
      implicitSummaries = analyser.summariseAll(implicitGraphs, new LinkedHashMap<>());
      implicitWalker = new ExceptionFlowWalker(implicitSummaries, analyser);
    }
    return implicitWalker;
  }

  /** Summaries behind {@link #getImplicitWalker()}; null until the walker has been built. */
  public static Map<String, MethodSummary> getImplicitSummaries() {
    getImplicitWalker();
    return implicitSummaries;
  }

  /**
   * Solves a method's observable paths the way Driver does: compose first, then solve everything,
   * so callee-propagated paths get a real verdict instead of none.
   */
  public static List<SolverResult> solveObservablePaths(final String methodSignature) {
    List<ExceptionPath> paths = getImplicitWalker().observablePaths(methodSignature);
    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(implicitSummaries);
    return solver
        .solveMethodPaths(Map.of(methodSignature, paths), new LinkedHashMap<>())
        .get(methodSignature);
  }

  private TestAnalysisContext() {}
}
