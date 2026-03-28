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

public class ArraySummaryTest {
  @Test
  public void getElementSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getElement(int[],int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr[i] != 0"));
    // formal params
    assertEquals(3, summary.formalParams().size());
    // is this correct? paramType is int[]
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    // return expr
    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr[i]", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void checkLengthSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int checkLength(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr.length != 0"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    // is this correct? paramType is int[]
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr.length", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void allocateSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] allocate(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("n >= 0"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    // is this correct? paramType is int[]
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals(1, summary.guardedReturn().size());
    assertEquals("newarray(int[])[n]", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void getStringElementSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: java.lang.String getStringElement(java.lang.String[],int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr[0] != 'abc'"));

    assertEquals(3, summary.formalParams().size());
    assertEquals("java.lang.String[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr[i]", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void getObjectElementSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: java.lang.Object getObjectElement(java.lang.Object[],int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr[0] != 'abc'"));

    assertEquals(3, summary.formalParams().size());
    assertEquals("java.lang.Object[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr[i]", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void getObjectFromArraySummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: br.unb.cic.witup.samples.Array$MyObject getObjectFromArray(br.unb.cic.witup.samples.Array$MyObject[],int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr[0].value <= 10"));

    assertEquals(3, summary.formalParams().size());
    assertEquals(
        "br.unb.cic.witup.samples.Array$MyObject[]",
        summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.OBJECT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr[i]", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void sumUntilZeroSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZero(int[])>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 >= arr.length"));

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("arr[i] != 0"));

    assertEquals(2, summary.formalParams().size());
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // returnExpr here is always giving 0 as we do not handle loop-carried acc
  }

  @Test
  public void sumUntilZeroWhileSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroWhile(int[])>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 >= arr.length"));

    assertEquals(2, summary.formalParams().size());
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("arr[i] != 0"));
    // returnExpr here is always giving 0 as we do not handle loop-carried acc
  }

  // might be worth writing about this case.
  @Test
  public void sumUntilZeroForEachSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroForEach(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 >= arr.length"));

    assertEquals(2, summary.formalParams().size());
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    assertFalse(path0.get(1).truthValue());
    // we can't "know" which temporary/local we'll get here with
    // before we process loops more thoroughly, so for now we'll
    // just assert there is a reference to some index in the array.
    // very cool that Z3 does not "care" about the jimple local not
    // tracing back fully; it still asserts on arr[i] and chooses i properly
    assertTrue(
        path0.get(1).symExpr().toString().matches(".*arr\\[.*].*"),
        "Expected array access, got: " + path0.get(1).symExpr());
  }

  @Test
  public void requireNonNullObjectSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: void requireNonNullObject(java.lang.Object)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.NullPointerException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("o != null"));

    assertEquals(2, summary.formalParams().size());
    assertEquals("java.lang.Object", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.OBJECT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    assertTrue(path0.getFirst().symExpr().toString().contains("o != null"), "Expected o != null");
  }

  @Test
  public void requireNonNullArraySummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] requireNonNullArray(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(
        "java.lang.NullPointerException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr != null"));

    assertEquals(2, summary.formalParams().size());
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void getCheckedSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getChecked(int[],int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(3, summary.exceptionPaths().size());
    // first path: arr == null
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("arr != null"));

    // second path: arr != null && i < 0.
    List<SymbolicConstraint> path1 = summary.exceptionPaths().get(1).getConstraints();
    assertTrue(path1.getFirst().truthValue());
    assertTrue(path1.getFirst().symExpr().toString().contains("arr != null"));

    assertFalse(path1.get(1).truthValue());
    assertTrue(path1.get(1).symExpr().toString().contains("i >= 0"));

    List<SymbolicConstraint> path3 = summary.exceptionPaths().get(2).getConstraints();
    assertTrue(path3.getFirst().truthValue());
    assertTrue(path3.getFirst().symExpr().toString().contains("arr != null"));

    assertTrue(path3.get(1).truthValue());
    assertTrue(path3.get(1).symExpr().toString().contains("i >= 0"));

    assertFalse(path3.get(2).truthValue());
    assertTrue(path3.get(2).symExpr().toString().contains("i < arr.length"));

    assertEquals(3, summary.formalParams().size());
    assertEquals("int[]", summary.formalParams().getFirst().getParamType());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    assertEquals(1, summary.guardedReturn().size());
    assertEquals("arr[i]", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void allocate2DSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[][] allocate2D(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(2, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("rows >= 0"));

    List<SymbolicConstraint> path1 = summary.exceptionPaths().get(1).getConstraints();
    assertTrue(path1.getFirst().truthValue());
    assertTrue(path1.getFirst().symExpr().toString().contains("rows >= 0"));

    assertFalse(path1.get(1).truthValue());
    assertTrue(path1.get(1).symExpr().toString().contains("cols >= 0"));

    assertEquals(3, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    assertEquals(1, summary.guardedReturn().size());
    assertEquals(
        "newmultiarray(int[][])[rows][cols]",
        summary.guardedReturn().getFirst().value().toString());
  }
}
