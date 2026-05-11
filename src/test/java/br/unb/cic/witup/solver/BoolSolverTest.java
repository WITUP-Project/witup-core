package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class BoolSolverTest {
  @Test
  public void toBooleanSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Bool: boolean toBoolean(java.lang.Integer,java.lang.Integer,java.lang.Integer)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertFalse(sol0.getBool("value_is_null"));
    assertFalse(sol0.getBool("value.equals(trueValue)"));
    assertFalse(sol0.getBool("value.equals(falseValue)"));

    SolverResult sol1 = analysis.solutions().get(1);

    assertTrue(sol1.isSat());
    assertTrue(sol1.getBool("value_is_null"));
    assertFalse(sol1.getBool("trueValue_is_null"));
    assertFalse(sol1.getBool("falseValue_is_null"));
  }

  @Test
  public void requireTrueIsUnsat() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: void requireTrue()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isUnsat());
  }

  @Test
  public void throwIfFalseCalleeIsUnsat() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: void throwIfFalseCallee()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    // Previously asserted Z3 returned UNSAT on this path. The summariser now catches the
    // direct (X, true) ∧ (X, false) contradiction structurally, so the path is dropped
    // before the solver runs. Same end state (path is impossible), shifted upstream.
    assertEquals(0, analysis.solutions().size());
  }
}
