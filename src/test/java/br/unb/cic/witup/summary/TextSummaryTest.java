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

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.RuntimeException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("s != 'abc'"));

    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.STRING, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    assertEquals(1, Integer.parseInt(summary.guardedReturn().getFirst().value().toString()));
  }

  @Test
  public void requireStringSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: java.lang.String requireString(java.lang.Object)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.RuntimeException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("s_instanceof_java_lang_String"));

    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.OBJECT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    assertTrue(
        summary.guardedReturn().getFirst().value().toString().contains("(java.lang.String)s"));
  }

  @Test
  public void invalidStringLengthSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.RuntimeException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("s.length() != 0"));

    assertEquals(1, Integer.parseInt(summary.guardedReturn().getFirst().value().toString()));
  }
}
