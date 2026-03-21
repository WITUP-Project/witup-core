package br.unb.cic.witup.solver;

import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MathSolverTest {
  @Test
  public void invalidFieldSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";
    SolverResult sol0 = TestAnalysisContext.getSolutions().get(methodSignature).getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("this.radius") < 0, "Expected radius <= 0");
  }

  @Test
  public void invalidParameterSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";
    SolverResult sol0 = TestAnalysisContext.getSolutions().get(methodSignature).getFirst();
    assertTrue(sol0.isSat());
    assertEquals(0, sol0.getInt("y"), "Expected y == 0");
  }

  @Test
  public void invalidParameterConjunctionSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";
    SolverResult sol0 = TestAnalysisContext.getSolutions().get(methodSignature).getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("p") < 0, "Expected p < 0");

    SolverResult sol1 = TestAnalysisContext.getSolutions().get(methodSignature).get(1);
    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("p") > 1, "Expected p > 1");
  }
}
