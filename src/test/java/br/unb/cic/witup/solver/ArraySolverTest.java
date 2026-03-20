package br.unb.cic.witup.solver;

import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.ModelValue;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArraySolverTest {
  @Test
  public void getElementSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getElement(int[],int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void checkLengthSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int checkLength(int[])>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    assertEquals(0, sol0.getInt("arr.length"), "arr.length should be 0");
  }

  @Test
  public void allocateSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] allocate(int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    assertTrue(sol0.getInt("n") < 0, "arr.length should be 0");

  }

  @Test
  public void getStringElementSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: java.lang.String getStringElement(java.lang.String[],int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");

    assertEquals("abc", elementValue.getString(), "arr[i] should be 0");

  }
}
