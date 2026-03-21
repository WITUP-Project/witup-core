package br.unb.cic.witup.solver;

import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoolSolverTest {
  @Test
  public void toBooleanSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: boolean toBoolean(java.lang.Integer,java.lang.Integer,java.lang.Integer)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(sol0.isSat());
    assertFalse(sol0.getBool("value_is_null"));
    assertFalse(sol0.getBool("value.equals(trueValue)"));
    assertFalse(sol0.getBool("value.equals(falseValue)"));

    SolverResult sol1 = TestAnalysisContext.getSolutions()
            .get(methodSignature).get(1);

    assertTrue(sol1.isSat());
    assertTrue(sol1.getBool("value_is_null"));
    assertFalse(sol1.getBool("trueValue_is_null"));
    assertFalse(sol1.getBool("falseValue_is_null"));
  }
}
