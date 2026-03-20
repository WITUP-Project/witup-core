package br.unb.cic.witup.intraprocedural;

import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NewIntTest {
  @Test
  public void addOverflow() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    List<SolverResult> results = TestAnalysisContext.getSolutions().get(methodSignature);
    assertNotNull(results);
    assertEquals(1, results.size());

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    int a = solution.getInt("a");
    int b = solution.getInt("b");
    assertTrue(a + b > 256, "Expected a + b > 256");

    MethodSummary summary = TestAnalysisContext.getSummaries().get(methodSignature);
    assertNotNull(summary);
    assertEquals(methodSignature, summary.getMethodSignature());
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
  }
}
