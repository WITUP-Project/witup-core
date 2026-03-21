package br.unb.cic.witup.summary;

import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MathSummaryTest {
  @Test
  public void invalidFieldSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("this.radius >= 0"));

    assertEquals(0, summary.getFormalParams().size());

    assertEquals("((3.14 * this.radius) * this.radius)", summary.getReturnExpr().toString());
  }

  @Test
  public void invalidParameterSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";

    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("y != 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());

    assertTrue(summary.getReturnExpr().toString().contains("x / y"));
  }

  @Test
  public void invalidParameterConjunctionSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";

    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
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

    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());

    assertEquals("p", summary.getReturnExpr().toString());
  }
}
