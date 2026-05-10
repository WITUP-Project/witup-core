package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class CallsSolverTest {
  @Test
  public void unguardedCalleeThrowSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void unguardedCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    // Single CALLEE_PROPAGATED path. Predicate is calleeThrows's `x < 0`, with x bound to
    // this method's parameter via formals→actuals substitution. The substitution should
    // resolve to the caller's own `x` parameter — model must concretely report x < 0.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
    assertTrue(
        sol.getInt("x") < 0,
        "callee predicate `x < 0` substituted to caller's x; SAT model must bind x to a negative int");
  }
}
