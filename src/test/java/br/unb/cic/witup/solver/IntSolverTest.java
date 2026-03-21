package br.unb.cic.witup.solver;

import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntSolverTest {
  @Test
  public void addOverflowSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    SolverResult solution = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") + solution.getInt("b") > 256, "expected a + b > 256");
  }

  @Test
  public void greaterThanConstantRhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";
    SolverResult solution = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") < 0, "expected a < 0");
  }

  @Test
  public void lesserThanConstantLhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
    SolverResult solution = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") < 0, " expected a < 0");
  }

  @Test
  public void equalsConstantRhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";

    SolverResult solution = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(solution.isSat());
    assertEquals(0, solution.getInt("a"), "expected a == 0");
  }

  @Test
  public void equalsConstantLhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";

    SolverResult solution = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(solution.isSat());
    assertEquals(0, solution.getInt("a"), "expected a == 0");
  }

  @Test
  public void negatedLessThanConstantRhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    SolverResult solution = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") <= 0, "expected a <= 0");
  }

  @Test
  public void lessThanConstantRhsViaBoolanSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isUnsat());

    SolverResult sol1 = TestAnalysisContext.getSolutions()
            .get(methodSignature).get(1);

    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("a") < 0, "Expected a < 0");
  }

  @Test
  public void lessThanConstantRhsViaNegatedBooleanSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") >= 0, "Expected a >= 0");

    SolverResult sol1 = TestAnalysisContext.getSolutions()
            .get(methodSignature).get(1);

    assertTrue(sol1.isUnsat());
  }

  @Test
  public void addAndCheckSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    assertTrue(sol0.getInt("a") + sol0.getInt("b") > 512, "Expected a + b > 512");
  }
}
