package br.unb.cic.witup.testinfra;

import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.solver.SolverResult;
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

  private TestAnalysisContext() {}
}
