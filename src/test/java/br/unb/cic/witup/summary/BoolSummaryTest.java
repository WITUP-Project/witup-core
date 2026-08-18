package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BoolSummaryTest {
  @Test
  public void toBooleanSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Bool: boolean toBoolean(java.lang.Integer,java.lang.Integer,java.lang.Integer)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(2, summary.exceptionPaths().size());
    // first path: value != null && !value.equals(trueValue) && !value.equals(falseValue)
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();

    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    assertEquals(3, path0.size());
    assertTrue(path0.get(0).truthValue());
    assertTrue(path0.get(0).symExpr().toString().contains("value != null"));

    // bool-method-equals-zero: "equals(...) returned false" — the constraint-level
    // stripBooleanEncoding peels `(b == 0, true)` to `(b, false)` for cleaner output.
    assertFalse(path0.get(1).truthValue());
    assertEquals("value.equals(trueValue)", path0.get(1).symExpr().toString());

    assertFalse(path0.get(2).truthValue());
    assertEquals("value.equals(falseValue)", path0.get(2).symExpr().toString());

    // second path: arr != null && i < 0.
    List<SymbolicConstraint> path1 = summary.exceptionPaths().get(1).getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().get(1).getExceptionQualifiedName());

    assertEquals(3, path1.size());

    assertFalse(path1.get(0).truthValue());
    assertTrue(path1.get(0).symExpr().toString().contains("value != null"));

    assertTrue(path1.get(1).truthValue());
    assertTrue(path1.get(1).symExpr().toString().contains("trueValue != null"));

    assertTrue(path1.get(2).truthValue());
    assertTrue(path1.get(2).symExpr().toString().contains("falseValue != null"));
  }

  @Test
  public void alwaysTrueSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: boolean alwaysTrue()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(0, summary.exceptionPaths().size());
    assertTrue(summary.hasReturnExpr());
    assertEquals(1, summary.guardedReturn().size());
    assertEquals("1", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void alwaysFalseSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: boolean alwaysFalse()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(0, summary.exceptionPaths().size());
    assertTrue(summary.hasReturnExpr());
    assertEquals(1, summary.guardedReturn().size());
    assertEquals("0", summary.guardedReturn().getFirst().value().toString());
  }

  @Test
  public void requireTrueSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: void requireTrue()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(1, summary.exceptionPaths().size());
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalStateException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());
    // throw fires when alwaysTrue() returns false. The constraint set should pin
    // a fresh _ret_X to alwaysTrue()'s guarded return (== 1) AND assert _ret_X is false.
    assertTrue(
        path0.size() >= 2,
        "expected at least a guarded-return precondition + the path constraint, got " + path0);
  }

  @Test
  public void throwIfFalseCalleeSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: void throwIfFalseCallee()>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    // The path through `throwIfFalseCallee` asserts both (X, true) and (X, false) on the
    // same SymExpr — a direct structural contradiction the summariser now prunes before
    // emission (previously this surfaced as an UNSAT solver verdict). No path emitted.
    assertEquals(0, summary.exceptionPaths().size());
  }
}
