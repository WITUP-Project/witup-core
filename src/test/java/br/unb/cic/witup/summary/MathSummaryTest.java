package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MathSummaryTest {
  @Test
  public void invalidFieldSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";

    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.RuntimeException",
        summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("this.radius >= 0"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().getFirst().getKind());

    assertEquals("((3.14 * this.radius) * this.radius)", summary.getGuardedReturn().getFirst().value().toString());
  }

  @Test
  public void invalidParameterSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";

    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.RuntimeException",
        summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("y != 0"));

    assertEquals(3, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(2).getKind());

    assertTrue(summary.getGuardedReturn().getFirst().value().toString().contains("x / y"));
  }

  @Test
  public void invalidParameterConjunctionSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";

    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(2, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.RuntimeException",
        summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(2, path0.size());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("p < 0"));

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("p <= 1"));

    List<SymbolicConstraint> path1 = summary.getExceptionPaths().get(1).getConstraints();
    assertEquals(1, path1.size());

    assertEquals(
        "java.lang.RuntimeException",
        summary.getExceptionPaths().get(1).getExceptionQualifiedName());

    assertTrue(path1.getFirst().truthValue());
    assertTrue(path1.getFirst().symExpr().toString().contains("p < 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertEquals("p", summary.getGuardedReturn().getFirst().value().toString());
  }

  @Test
  public void truncateSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncate(double)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.RuntimeException",
        summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("truncated >= 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.REAL, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertEquals("truncated", summary.getGuardedReturn().getFirst().value().toString());
  }

  @Test
  public void truncateInlineSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncateInline(double)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.RuntimeException",
        summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(int)d >= 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.REAL, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertEquals("(int)d", summary.getGuardedReturn().getFirst().value().toString());
  }
}
