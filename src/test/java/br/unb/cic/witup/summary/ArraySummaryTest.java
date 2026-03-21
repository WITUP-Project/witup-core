package br.unb.cic.witup.summary;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
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

  @Test
  public void getObjectFromArraySummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: br.unb.cic.witup.samples.Array$MyObject getObjectFromArray(br.unb.cic.witup.samples.Array$MyObject[],int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr[0].value <= 10"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals("br.unb.cic.witup.samples.Array$MyObject[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());

    assertEquals("arr[i]", summary.getReturnExpr().toString());
  }

  @Test
  public void sumUntilZeroSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZero(int[])>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("0 >= arr.length"));

    assertFalse(path0.get(1).getTruthValue());
    assertTrue(path0.get(1).getSymExpr().toString().contains("arr[i] != 0"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());

    // returnExpr here is always giving 0 as we do not handle loop-carried acc
  }

  @Test
  public void sumUntilZeroWhileSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroWhile(int[])>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("0 >= arr.length"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());

    assertFalse(path0.get(1).getTruthValue());
    assertTrue(path0.get(1).getSymExpr().toString().contains("arr[i] != 0"));
    // returnExpr here is always giving 0 as we do not handle loop-carried acc
  }

  // might be worth writing about this case.
  @Test
  public void sumUntilZeroForEachSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroForEach(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("0 >= arr.length"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());

    assertFalse(path0.get(1).getTruthValue());
    // we can't "know" which temporary/local we'll get here with
    // before we process loops more thoroughly, so for now we'll
    // just assert there is a reference to some index in the array.
    // very cool that Z3 does not "care" about the jimple local not
    // tracing back fully; it still asserts on arr[i] and chooses i properly
    assertTrue(path0.get(1).getSymExpr().toString().matches(".*arr\\[.*\\].*"),
            "Expected array access, got: " + path0.get(1).getSymExpr());
  }

  @Test
  public void requireNonNullObjectSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: void requireNonNullObject(java.lang.Object)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("o != null"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals("java.lang.Object", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(0).getKind());

    assertTrue(path0.get(0).getSymExpr().toString().contains("o != null"),
            "Expected o != null");
  }

  @Test
  public void requireNonNullArraySummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] requireNonNullArray(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr != null"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());

    assertEquals("arr", summary.getReturnExpr().toString());
  }

  @Test
  public void getCheckedSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getChecked(int[],int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(3, summary.getSymbolicConstraintPaths().size());
    // first path: arr == null
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("arr != null"));

    // second path: arr != null && i < 0.
    List<SymbolicConstraint> path1 = summary.getSymbolicConstraintPaths().get(1);
    assertTrue(path1.get(0).getTruthValue());
    assertTrue(path1.get(0).getSymExpr().toString().contains("arr != null"));

    assertFalse(path1.get(1).getTruthValue());
    assertTrue(path1.get(1).getSymExpr().toString().contains("i >= 0"));

    List<SymbolicConstraint> path3 = summary.getSymbolicConstraintPaths().get(2);
    assertTrue(path3.get(0).getTruthValue());
    assertTrue(path3.get(0).getSymExpr().toString().contains("arr != null"));

    assertTrue(path3.get(1).getTruthValue());
    assertTrue(path3.get(1).getSymExpr().toString().contains("i >= 0"));

    assertFalse(path3.get(2).getTruthValue());
    assertTrue(path3.get(2).getSymExpr().toString().contains("i < arr.length"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals("int[]", summary.getFormalParams().get(0).getParamType());
    assertEquals(SymKind.INT, summary.getFormalParams().get(0).getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());

    assertEquals("arr[i]", summary.getReturnExpr().toString());
  }
}
