package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.solver.model.IntValue;
import br.unb.cic.witup.solver.model.StringValue;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class TextSolverTest {
  @Test
  public void invalidStringSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertEquals(new StringValue("abc"), sol0.modelValueMap().get("s"));
  }

  @Test
  public void requireStringSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: java.lang.String requireString(java.lang.Object)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertFalse(sol0.getBool("s_instanceof_java_lang_String"));
  }

  @Test
  public void invalidStringLengthSolution() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertEquals(new IntValue(0), sol0.modelValueMap().get("s.length()"));
  }
}
