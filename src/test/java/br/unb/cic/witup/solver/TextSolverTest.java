package br.unb.cic.witup.solver;

import br.unb.cic.witup.solver.model.StringValue;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextSolverTest {
  @Test
  public void invalidStringSolution() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";

    SolverResult sol0 = TestAnalysisContext.getSolutions().get(methodSignature).getFirst();
    assertTrue(sol0.isSat());
    assertEquals(new StringValue("abc"), sol0.modelValueMap().get("s"));
  }
}
