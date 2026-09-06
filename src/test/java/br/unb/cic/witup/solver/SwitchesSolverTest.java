package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SwitchesSolverTest {

  private static final String CLASSIFY =
      "<br.unb.cic.witup.samples.Switches: int classify(java.lang.String,int)>";

  @Test
  public void everySwitchArmFlowGetsASatisfiableWitness() {
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(CLASSIFY);
    List<SolverResult> solutions = analysis.solutions();

    assertEquals(3, solutions.size(), "one verdict per throw: the guard and both switch arms");
    for (SolverResult sol : solutions) {
      assertTrue(sol.isSat(), "every arm is reachable, so none of these may be refuted");
    }
  }
}
