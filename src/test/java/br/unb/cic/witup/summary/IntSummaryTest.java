package br.unb.cic.witup.summary;

import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntSummaryTest {
  @Test
  public void addOverFlowSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("(a + b) <= 256"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    // return expr
    assertTrue(summary.getReturnExpr().toString().contains("a + b"));
  }

  @Test
  public void greaterThanConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("a >= 0"));
    // formal params
    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void lesserThanConstantLhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("0 <= a"));
    // formal params
    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void equalsConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("a != 0"));
    // formal params
    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }
}
