package br.unb.cic.witup.analysis.loop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class LoopSolverTest {

  @Test
  public void sumThresholdIsReachable() {
    String sig = "<br.unb.cic.witup.samples.LoopSamples: void sumThreshold(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(sig);
    assertFalse(analysis.solutions().isEmpty(), "expected at least one path");
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat(), "sum > 256 should be reachable (sum is unconstrained)");
  }

  @Test
  public void inductionVarThrowIsReachable() {
    String sig = "<br.unb.cic.witup.samples.LoopSamples: void inductionVarThrow(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(sig);
    assertFalse(analysis.solutions().isEmpty(), "expected at least one path");
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat(), "arr[i] < 0 should be reachable");
  }
}
