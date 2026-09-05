package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CallsSolverTest {
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

  @Test
  public void composedCalleeThrowGetsItsOwnVerdict() {
    // calleeMayThrow raises IAE when x < 0, and unguardedCalleeThrow does not catch it. The
    // composed path is feasible, so it must come back SAT with a model
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void unguardedCalleeThrow(int)>";
    List<SolverResult> results = TestAnalysisContext.solveObservablePaths(methodSignature);

    assertEquals(1, results.size(), "one uncaught callee flow escapes this method");
    SolverResult composed = results.getFirst();
    assertEquals(SolverStatus.SAT, composed.getStatus());
    assertFalse(
        composed.getModelValueMap().isEmpty(),
        "a SAT composed path must yield argument values that make the caller throw");
  }

  @Test
  public void composedPathContradictoryAfterSubstitutionIsRefuted() {
    // requireOrdered throws iff lo > hi; alwaysOrdered passes the same value for both, so the
    // composed predicate is `x > x`. only the solver can decide
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void alwaysOrdered(int)>";
    List<SolverResult> results = TestAnalysisContext.solveObservablePaths(methodSignature);

    assertEquals(1, results.size());
    assertEquals(
        SolverStatus.UNSAT,
        results.getFirst().getStatus(),
        "substitution made the callee's predicate unsatisfiable");
  }
}
