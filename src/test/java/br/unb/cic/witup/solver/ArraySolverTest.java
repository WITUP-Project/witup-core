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
}
