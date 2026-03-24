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

public class IntSummaryTest {
  @Test
  public void addOverFlowSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getExceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();

    assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) <= 256"));
    // formal params
    assertEquals(3, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(2).getKind());

    // return expr
    assertTrue(summary.getReturnExpr().toString().contains("a + b"));
  }

  @Test
  public void greaterThanConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getExceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a >= 0"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void lesserThanConstantLhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getExceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 <= a"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void equalsConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getExceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a != 0"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void equalsConstantLhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getExceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 != a"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void negatedLessThanConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(1, summary.getExceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a > 0"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.getReturnExpr().toString());
  }

  @Test
  public void lessThanConstantRhsViaBooleanSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    // constraint paths
    assertEquals(2, summary.getExceptionPaths().size());
    // constraint path 0: (a >= 0, true) -> (0 == 0, false), UNSAT
    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a >= 0"));
    // constraint path 1: (a >= 0, false) -> (1 == 0, false), SAT
    List<SymbolicConstraint> path1 = summary.getExceptionPaths().get(1).getConstraints();
    assertFalse(path1.getFirst().truthValue());
    assertTrue(path1.getFirst().symExpr().toString().contains("a >= 0"));
    assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().get(1).getExceptionQualifiedName());

    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr too complicated already
    // (((a >= 0) ? ((0 == 0) ? 1 : 0) : 0) ? a : a)
    // but correct and z3 handles
  }

  @Test
  public void lessThanConstantRhsViaNegatedBooleanSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    assertEquals(2, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a >= 0"));

    List<SymbolicConstraint> path1 = summary.getExceptionPaths().get(1).getConstraints();
    assertFalse(path1.getFirst().truthValue());
    assertTrue(path1.getFirst().symExpr().toString().contains("a >= 0"));
    // formal params
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    // return expr
    // (((a >= 0) ? ((0 != 0) ? 1 : 0) : 0) ? a : a)
  }

  @Test
  public void addAndCheckSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(2, path0.size());
    // SymIte is (((((a + b) <= 256) == 0) ? 1 : 0) ? 0 : 1), truthValue=true
    assertTrue(path0.getFirst().truthValue());
    // should contain (a + b) <= 256 as this is the condition in the callee
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) <= 256"));

    // condition from within the method.
    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("(a + b) <= 512"));

    assertEquals(3, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(2).getKind());

    // massive SymITE
    assertTrue(summary.getReturnExpr().toString().contains("a + b"));
  }

  @Test
  public void negateValueSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negateValue(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    // condition from callee
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a != 0"));

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("a != 0"));
    // too hard to assert on ITE trees when doing interprocedural.
    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());
  }

  @Test
  public void applyAndCheckSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: java.lang.Integer lambda$applyAndCheck$0(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    // too hard to assert on ITE trees when doing interprocedural.

    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
  }

  @Test
  public void applyAndCheckResultSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int applyAndCheckResult(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(x * 2) >= 0"));

    assertEquals(2, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(1).getKind());

    assertTrue(summary.getReturnExpr().toString().contains("x * 2"));
  }

  @Test
  public void callStaticAddSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int callStaticAdd(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(2, path0.size());
    // condition from callee
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) <= 256"));

    // condition from caller
    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("(a + b) <= 512"));

    assertEquals(3, summary.getFormalParams().size());
    assertEquals(SymKind.INT, summary.getFormalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.getFormalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(2).getKind());

    assertTrue(summary.getReturnExpr().toString().contains("a + b"));
  }

  @Test
  public void callStaticAddLteSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int callStaticAddLte(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.getExceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.getExceptionPaths().getFirst().getConstraints();
        assertEquals("java.lang.IllegalArgumentException", summary.getExceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(2, path0.size());
    // condition from callee. it throws if a + b <= 256
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) > 256"));

    // condition from caller. it throws if result == (a + b) > 512
    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("(a + b) <= 512"));
  }
}
