package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class CatchesSummaryTest {
  @Test
  public void simpleCatchSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleCatch(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    dumpPaths("simpleCatch", summary);
  }

  @Test
  public void loopCatchSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void loopCatch(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    dumpPaths("loopCatch", summary);

    // With visited-edges path enumeration the back-edge through the loop body is reachable,
    // so we expect the catch-fired path (the one carrying the @caughtexception constraint)
    // to appear alongside the empty-loop path.
    boolean hasCaughtPath =
        summary.exceptionPaths().stream()
            .anyMatch(
                ep ->
                    ep.getConstraints().stream()
                        .anyMatch(c -> c.symExpr().toString().contains("caughtexception")));
    org.junit.jupiter.api.Assertions.assertTrue(
        hasCaughtPath, "expected at least one path carrying a @caughtexception constraint");
  }

  private static void dumpPaths(final String label, final MethodSummary summary) {
    System.out.println(label + " exceptionPaths (" + summary.exceptionPaths().size() + "):");
    for (int p = 0; p < summary.exceptionPaths().size(); p++) {
      System.out.println(
          " path " + p + " (" + summary.exceptionPaths().get(p).getExceptionQualifiedName() + "):");
      var cs = summary.exceptionPaths().get(p).getConstraints();
      for (int i = 0; i < cs.size(); i++) {
        System.out.println(
            "   [" + i + "] truth=" + cs.get(i).truthValue() + " expr=" + cs.get(i).symExpr());
      }
    }
  }
}
