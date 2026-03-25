package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class MathSolverTest {
  @Test
  public void invalidFieldSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("this.radius") < 0, "Expected radius <= 0");
  }

  @Test
  public void invalidParameterSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertEquals(0, sol0.getInt("y"), "Expected y == 0");
  }

  @Test
  public void invalidParameterConjunctionSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";
    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(methodSignature);

    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("p") > 1, "Expected p > 1");

    SolverResult sol1 = analysis.solutions().get(1);
    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("p") < 0, "Expected p < 0");
  }

  @Test
  public void truncateSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncate(double)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("truncated") < 0, "Expected truncated < 0");
  }

  @Test
  public void truncateInlineSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncateInline(double)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("(int)d") < 0, "Expected (int)d < 0");
  }
}
