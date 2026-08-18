package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class IntSolverTest {
  @Test
  public void addOverflowSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") + sol0.getInt("b") > 256, "expected a + b > 256");
  }

  @Test
  public void greaterThanConstantRhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") < 0, "expected a < 0");
  }

  @Test
  public void lesserThanConstantLhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") < 0, " expected a < 0");
  }

  @Test
  public void equalsConstantRhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertEquals(0, sol0.getInt("a"), "expected a == 0");
  }

  @Test
  public void equalsConstantLhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertEquals(0, sol0.getInt("a"), "expected a == 0");
  }

  @Test
  public void negatedLessThanConstantRhsSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") <= 0, "expected a <= 0");
  }

  @Test
  public void lessThanConstantRhsViaBoolanSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    // Pre-pruning we had two paths: UNSAT (boolean-indirection artifact) and SAT (a < 0
    // triggers throw). 1b prunes the UNSAT path at the summariser; only the SAT survives.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") < 0, "Expected a < 0");
  }

  @Test
  public void lessThanConstantRhsViaNegatedBooleanSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    // Symmetric to viaBoolean: SAT path requires a >= 0; UNSAT artifact pruned by 1b.
    assertEquals(1, analysis.solutions().size());
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") >= 0, "Expected a >= 0");
  }

  @Test
  public void addAndCheckSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    // calle throws if a + b > 256
    // so it is not possible for result > 512 to throw
    assertTrue(sol0.isUnsat());
  }

  @Test
  public void negateValueSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negateValue(int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());

    assertTrue(sol0.getInt("a") > 0);
  }

  @Test
  public void applyAndCheckSolution() {
    String lambdaSig =
        "<br.unb.cic.witup.samples.Int: java.lang.Integer lambda$applyAndCheck$0(int)>";

    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(lambdaSig);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("x") < 0, "Expected x < 0");
  }

  @Test
  public void applyAndCheckResultSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int applyAndCheckResult(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("x") < 0, "Expected x < 0 (since (x * 2 < 0))");
  }

  @Test
  public void callStaticAddSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int callStaticAdd(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    // calle throws if a + b > 256, so a + b > 512 cannot throw.
    assertTrue(sol0.isUnsat());
  }

  @Test
  public void callStaticAddLteSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int callStaticAddLte(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    // from the callee, (a + b) must be > 256
    // from the caller, (a + b) must be <= 512
    assertTrue(sol0.getInt("a") + sol0.getInt("b") > 256, "Expected a + b > 256 to get here");
    assertTrue(sol0.getInt("a") + sol0.getInt("b") > 512, "Expected a + b <= 512 to throw");
  }
}
