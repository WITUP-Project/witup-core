package br.unb.cic.witup.solver;

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
}
