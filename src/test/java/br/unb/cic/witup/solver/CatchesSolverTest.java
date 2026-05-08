package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class CatchesSolverTest {

  @Test
  public void simpleCatchSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleCatch(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);

    // path 0: catch fired, post-catch throw fires when exception is non-null
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertFalse(sol0.getBool("exception_is_null"), "catch-fired path requires exception non-null");

    // unsat if path-aware joining works
    SolverResult sol1 = analysis.solutions().get(1);
    assertTrue(sol1.isUnsat());
  }

  @Test
  public void loopCatchSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void loopCatch(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);

    // path 0: loop entered, catch fired on the iteration, then post-loop throw.
    // Per-iteration symbolization makes loop-entry (i#0 < xs.length) and loop-exit
    // (i#1 >= xs.length) consistent, so this path is SAT (previously UNSAT-bogus).
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertFalse(sol0.getBool("exception_is_null"), "catch-fired path requires exception non-null");

    // unsat if path-aware joining works
    SolverResult sol1 = analysis.solutions().get(1);
    assertTrue(sol1.isUnsat());

    // unsat if path-aware joining works
    SolverResult sol2 = analysis.solutions().get(2);
    assertTrue(sol2.isUnsat());
  }
}
