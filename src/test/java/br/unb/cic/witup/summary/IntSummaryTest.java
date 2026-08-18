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
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();

    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) <= 256"));
    // formal params
    assertEquals(3, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    // return expr
    assertTrue(summary.guardedReturn().getFirst().value().toString().contains("a + b"));
  }

  @Test
  public void greaterThanConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a >= 0"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void lesserThanConstantLhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 <= a"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void equalsConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a != 0"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void equalsConstantLhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("0 != a"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void negatedLessThanConstantRhsSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // constraint paths
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a > 0"));
    // formal params
    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    // return expr
    assertEquals("a", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void lessThanConstantRhsViaBooleanSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // Pre-pruning we emitted two paths: (a >= 0, true) -> (0 == 0, false) which was UNSAT
    // by structural contradiction, and (a >= 0, false) -> (1 == 0, false) the SAT path.
    // 1b's constant folding now drops the UNSAT path at summariser time and removes the
    // trivially-satisfied (1 == 0, false) tautology from the SAT path. Single path remains.
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a >= 0"));

    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());
  }

  @Test
  public void lessThanConstantRhsViaNegatedBooleanSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());
    // Symmetric to viaBoolean: pre-pruning we emitted two paths, one SAT with (a >= 0,
    // true) and one UNSAT with a `0 != 0` artifact from the boolean indirection. 1b's
    // constant folding drops the UNSAT path at summariser time. Single SAT path remains.
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a >= 0"));

    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());
  }

  @Test
  public void addAndCheckSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(1, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(3, path0.size());
    // constraint 0: guarded binding — if callee doesn't throw, _ret_0 = (a + b)
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) <= 256"));

    assertTrue(path0.getFirst().symExpr().toString().contains("_ret_"));
    assertTrue(path0.getFirst().symExpr().toString().contains("((a + b) <= 256)"));
    assertTrue(path0.getFirst().symExpr().toString().contains("((a + b) <= 256)"));
    assertTrue(path0.getFirst().symExpr().toString().contains("== (a + b)"));

    // constraint 1: negated callee throw path — callee's throw condition must be false
    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("(a + b) <= 256"));

    // constraint 2: caller's branch condition using fresh var
    assertFalse(path0.get(2).truthValue());
    assertTrue(path0.get(2).symExpr().toString().contains("_ret_"));
    assertTrue(path0.get(2).symExpr().toString().contains("<= 512"));

    assertEquals(3, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    //    assertTrue(summary.getGuardedReturn().getFirst().value().toString().contains("_ret_"));
  }

  @Test
  public void negateValueSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negateValue(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(4, path0.size());
    // condition from callee
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("a != 0"));

    assertTrue(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("a != 0"));

    assertFalse(path0.get(2).truthValue());
    assertTrue(path0.get(2).symExpr().toString().contains("a != 0"));

    assertFalse(path0.get(3).truthValue());
    assertTrue(path0.get(3).symExpr().toString().contains(">= 0"));

    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());
  }

  @Test
  public void applyAndCheckSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: java.lang.Integer lambda$applyAndCheck$0(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertFalse(path0.getFirst().truthValue());
    // too hard to assert on ITE trees when doing interprocedural.

    assertEquals(1, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
  }

  @Test
  public void applyAndCheckResultSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int applyAndCheckResult(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(2, path0.size());

    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(x * 2)"));

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains(">= 0"));

    assertEquals(2, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(1).getKind());

    //    assertTrue(summary.getGuardedReturn().getFirst().value().toString().contains("x * 2"));
  }

  @Test
  public void callStaticAddSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int callStaticAdd(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(3, path0.size());
    // condition from callee
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) <= 256"));

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("<= 256"));

    assertFalse(path0.get(2).truthValue());
    assertTrue(path0.get(2).symExpr().toString().contains("<= 512"));

    assertEquals(3, summary.formalParams().size());
    assertEquals(SymKind.INT, summary.formalParams().getFirst().getKind());
    assertEquals(SymKind.INT, summary.formalParams().get(1).getKind());
    assertEquals(SymKind.OBJECT, summary.formalParams().get(2).getKind());

    //    assertTrue(summary.getGuardedReturn().getFirst().value().toString().contains("a + b"));
  }

  @Test
  public void callStaticAddLteSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int callStaticAddLte(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    assertEquals(1, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(3, path0.size());
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("(a + b) > 256"));

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("(a + b) > 256"));

    assertFalse(path0.get(2).truthValue());
  }
}
