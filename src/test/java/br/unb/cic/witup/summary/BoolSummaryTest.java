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
    assertEquals(methodSignature, summary.getMethodSignature());

    assertEquals(2, summary.getSymbolicConstraintPaths().size());
    // first path: value != null && !value.equals(trueValue) && !value.equals(falseValue)
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertEquals(3, path0.size());
    assertTrue(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("value != null"));

    assertFalse(path0.get(1).getTruthValue());
    assertTrue(path0.get(1).getSymExpr().toString().contains("value.equals(trueValue)"));

    assertFalse(path0.get(2).getTruthValue());
    assertTrue(path0.get(2).getSymExpr().toString().contains("value.equals(falseValue)"));

    // second path: arr != null && i < 0.
    List<SymbolicConstraint> path1 = summary.getSymbolicConstraintPaths().get(1);
    assertEquals(3, path1.size());

    assertFalse(path1.get(0).getTruthValue());
    assertTrue(path1.get(0).getSymExpr().toString().contains("value != null"));

    assertTrue(path1.get(1).getTruthValue());
    assertTrue(path1.get(1).getSymExpr().toString().contains("trueValue != null"));

    assertTrue(path1.get(2).getTruthValue());
    assertTrue(path1.get(2).getSymExpr().toString().contains("falseValue != null"));
  }
}
