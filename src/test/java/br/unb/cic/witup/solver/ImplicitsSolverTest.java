package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    // Single implicit-NPE path. Predicate is "receiver == null"; with no other constraints
    // along the path, the solver should report SAT.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
  }

  @Test
  public void fieldNpeSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int fieldNpe(br.unb.cic.witup.samples.Implicits$Box)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    // Same shape as receiverNpe — single straight path through the body, predicate is the
    // receiver-null check on the field's base.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
  }

  @Test
  public void arrayDerefSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int arrayDeref(int[])>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    // Two implicit paths from a single `arr[0]` access:
    // sol0: NPE — predicate `arr == null`, trivially SAT.
    // sol1: AIOOBE — `arr != null AND (0 < 0 || 0 >= arr.length)`. The disjunct `0 < 0`
    //       is UNSAT, so SAT requires `0 >= arr.length`, i.e. arr is non-null and empty.
    assertEquals(2, analysis.solutions().size());

    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());

    SolverResult sol1 = analysis.solutions().get(1);
    assertTrue(sol1.isSat());
  }

  @Test
  public void negativeArraySizeSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int[] negativeArraySize(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    // Predicate is `n < 0`, trivially SAT for any signed int parameter.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
  }

  @Test
  public void divByZeroSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int divByZero(int,int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);

    // Predicate is `b == 0`, trivially SAT.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol = analysis.solutions().getFirst();
    assertTrue(sol.isSat());
  }
}
