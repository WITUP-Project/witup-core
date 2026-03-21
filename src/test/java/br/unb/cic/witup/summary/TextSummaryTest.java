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

public class TextSummaryTest {
  @Test
  public void invalidStringSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";

    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());

    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("s != 'abc'"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.STRING, summary.getFormalParams().get(0).getKind());

    assertEquals(1, Integer.parseInt(summary.getReturnExpr().toString()));
  }

  @Test
  public void requireStringSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Text: java.lang.String requireString(java.lang.Object)>";

    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("s_instanceof_java_lang_String"));

    assertEquals(1, summary.getFormalParams().size());
    assertEquals(SymKind.OBJECT, summary.getFormalParams().get(0).getKind());

    assertTrue(summary.getReturnExpr().toString().contains("(java.lang.String)s"));
  }

  @Test
  public void invalidStringLengthSummary() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);

    assertEquals(1, summary.getSymbolicConstraintPaths().size());
    List<SymbolicConstraint> path0 = summary.getSymbolicConstraintPaths().get(0);
    assertFalse(path0.get(0).getTruthValue());
    assertTrue(path0.get(0).getSymExpr().toString().contains("s.length() != 0"));

    assertEquals(1, Integer.parseInt(summary.getReturnExpr().toString()));
  }
}
