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

    assertFalse(path0.get(1).truthValue());
    assertTrue(path0.get(1).symExpr().toString().contains("value.equals(trueValue)"));

    assertFalse(path0.get(2).truthValue());
    assertTrue(path0.get(2).symExpr().toString().contains("value.equals(falseValue)"));

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
}
