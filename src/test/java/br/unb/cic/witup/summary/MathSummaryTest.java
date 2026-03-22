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

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("this.radius >= 0"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(0).getKind());

    assertEquals("((3.14 * this.radius) * this.radius)", summary.getReturnExpr().toString());
  }

  @Test
  public void invalidParameterSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";

    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    ;

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("y != 0"));

    assertEquals(3, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(2).getKind());

    assertTrue(summary.getReturnExpr().toString().contains("x / y"));
  }

  @Test
  public void invalidParameterConjunctionSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";

    ProjectAnalyser pa = TestAnalysisContext.getAnalyser();
    AnalysisResult analysis = pa.analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(2, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertTrue(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("p < 0"));

    List<SymbolicConstraint> path1 = summary.getSymbolicConstraintPaths().get(1);
    assertFalse(path1.get(0).getTruthValue());
    assertTrue(path1.get(0).getSymExpr().toString().contains("p < 0"));

    assertFalse(path1.get(1).getTruthValue());
    assertTrue(path1.get(1).getSymExpr().toString().contains("p <= 1"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());


    assertEquals("p", summary.getReturnExpr().toString());
  }

  @Test
  public void truncateSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncate(double)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("truncated >= 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.REAL, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());


    assertEquals("truncated", summary.getReturnExpr().toString());
  }

  @Test
  public void truncateInlineSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncateInline(double)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("(int)d >= 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.REAL, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertEquals("(int)d", summary.getReturnExpr().toString());
  }
}
