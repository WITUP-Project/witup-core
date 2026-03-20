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

public class ArraySummaryTest {
  @Test
  public void getElementSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getElement(int[],int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr[i] != 0"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    // is this correct? paramType is int[]
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    // return expr
    assertEquals("arr[i]", summary.getReturnExpr().toString());
  }

  @Test
  public void checkLengthSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int checkLength(int[])>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr.length != 0"));
    // formal params
    assertEquals(1, summary.getFormalParams().size());
    // is this correct? paramType is int[]
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    // return expr
    assertEquals("arr.length", summary.getReturnExpr().toString());
  }

  @Test
  public void allocateSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] allocate(int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);

    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("n >= 0"));
    // formal params
    assertEquals(1, summary.getFormalParams().size());
    // is this correct? paramType is int[]
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    // return expr
    assertEquals("newarray(int[])[n]", summary.getReturnExpr().toString());
  }

  @Test
  public void getStringElementSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: java.lang.String getStringElement(java.lang.String[],int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr[0] != 'abc'"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals("java.lang.String[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals("arr[i]", summary.getReturnExpr().toString());
  }

  @Test
  public void getObjectElementSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: java.lang.Object getObjectElement(java.lang.Object[],int)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr[0] != 'abc'"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals("java.lang.Object[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals("arr[i]", summary.getReturnExpr().toString());
  }
}
