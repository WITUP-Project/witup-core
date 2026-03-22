package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TextSummaryTest {
  @Test
  public void invalidStringSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("s != 'abc'"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.STRING, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertEquals(1, Integer.parseInt(summary.getReturnExpr().toString()));
  }

  @Test
  public void requireStringSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: java.lang.String requireString(java.lang.Object)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("s_instanceof_java_lang_String"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertTrue(summary.getReturnExpr().toString().contains("(java.lang.String)s"));
  }

  @Test
  public void invalidStringLengthSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("s.length() != 0"));

    assertEquals(1, Integer.parseInt(summary.getReturnExpr().toString()));
  }
}
