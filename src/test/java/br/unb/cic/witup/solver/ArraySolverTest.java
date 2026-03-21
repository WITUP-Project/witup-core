package br.unb.cic.witup.solver;

import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.ModelValue;
import br.unb.cic.witup.solver.model.ObjectValue;
import br.unb.cic.witup.solver.model.StringValue;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

  @Test
  public void getObjectElementSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: java.lang.Object getObjectElement(java.lang.Object[],int)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()

            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertNotNull(elementValue);
    assertTrue(elementValue instanceof ObjectValue || elementValue instanceof StringValue);
  }

  @Test
  public void getObjectFromArraySolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: br.unb.cic.witup.samples.Array$MyObject getObjectFromArray(br.unb.cic.witup.samples.Array$MyObject[],int)>";
    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    ModelValue fieldValue = elementValue.getField("value");

    assertTrue(fieldValue.getInt() > 10, "expected arr[0].value <= 10");
  }

  @Test
  public void sumUntilZeroSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZero(int[])>";
    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void sumUntilZeroDoWhileSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroDoWhile(int[])>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void sumUntilZeroForEachSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroForEach(int[])>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void requireNonNullObjectSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: void requireNonNullObject(java.lang.Object)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();

    assertTrue(sol0.isSat());
    assertEquals(0, sol0.getInt("o"), "o to be 0 (encoding for null)");
  }

  @Test
  public void requireNonNullArraySolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] requireNonNullArray(int[])>";

    SolverResult sol0 = TestAnalysisContext.getSolutions()
            .get(methodSignature).getFirst();
    assertTrue(sol0.isSat());

    assertTrue(sol0.getBool("arr_is_null"), "arr_is_null to be true");
  }

}
