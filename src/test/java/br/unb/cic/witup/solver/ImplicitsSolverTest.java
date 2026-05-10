package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class ImplicitsSolverTest {
  @Test
  public void receiverNpeSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int receiverNpe(java.lang.Object)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
    assertTrue(sol.getBool("o_is_null"), "receiver o must be null for the implicit NPE");
  }

  @Test
  public void fieldNpeSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int fieldNpe(br.unb.cic.witup.samples.Implicits$Box)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
    assertTrue(sol.getBool("b_is_null"), "field receiver b must be null for the implicit NPE");
  }

  @Test
  public void arrayDerefSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int arrayDeref(int[])>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    // Two implicit paths from a single `arr[0]` access:
    // sol0: NPE — `arr == null`.
    // sol1: AIOOBE — `arr != null AND (0 < 0 || 0 >= arr.length)`. `0 < 0` is UNSAT, so
    //       SAT requires `0 >= arr.length`, i.e. arr is non-null and length <= 0.
    assertEquals(2, analysis.solutions().size());

    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getBool("arr_is_null"), "NPE path: arr must be null");

    SolverResult sol1 = analysis.solutions().get(1);
    assertTrue(sol1.isSat());
    assertFalse(sol1.getBool("arr_is_null"), "AIOOBE path: arr must NOT be null");
    assertTrue(
        sol1.getInt("arr.length") <= 0,
        "AIOOBE path with index 0: arr.length must satisfy 0 >= arr.length");
  }

  @Test
  public void negativeArraySizeSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int[] negativeArraySize(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
    assertTrue(sol.getInt("n") < 0, "n must be negative to trigger NegativeArraySizeException");
  }

  @Test
  public void divByZeroSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int divByZero(int,int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
    assertEquals(0, sol.getInt("b"), "divisor b must be 0 to trigger ArithmeticException");
  }
}
