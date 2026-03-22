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
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isUnsat());

    SolverResult sol1 = analysis.solutions().get(1);

    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("a") < 0, "Expected a < 0");
  }

  @Test
  public void lessThanConstantRhsViaNegatedBooleanSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();

    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") >= 0, "Expected a >= 0");

    SolverResult sol1 = analysis.solutions().get(1);

    assertTrue(sol1.isUnsat());
  }

  @Test
  public void addAndCheckSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());

    assertTrue(sol0.getInt("a") + sol0.getInt("b") > 512, "Expected a + b > 512");
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
    System.out.println("model: " + sol0.getModelValueMap());
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
}
