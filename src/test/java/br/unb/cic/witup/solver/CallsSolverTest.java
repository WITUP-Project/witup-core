package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class CallsSolverTest {
  // Both methods have no own throws — the summary is empty, so the solver has nothing to
  // solve here. The transitive caller-observable contract (and the constraint solving for
  // each propagated path) lives at the ExceptionFlowWalker level, not in MethodSummary.

  @Test
  public void unguardedCalleeThrowSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void unguardedCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    assertEquals(0, analysis.solutions().size());
  }

  @Test
  public void caughtCalleeThrowSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void caughtCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    assertEquals(0, analysis.solutions().size());
  }
}
