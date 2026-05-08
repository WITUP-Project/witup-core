package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FileSolverTest {

  @Test
  public void verifiedListFilesSolutions() {
    String methodSignature =
        "<br.unb.cic.witup.samples.File: java.io.File[] verifiedListFiles(java.io.File)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    List<SolverResult> sols = analysis.solutions();
    assertEquals(3, sols.size());

    // path 0: throw when !exists()
    SolverResult s0 = sols.get(0);
    assertTrue(s0.isSat());
    assertFalse(s0.getBool("directory.exists()"));

    // path 1: throw when exists() && !isDirectory()
    SolverResult s1 = sols.get(1);
    assertTrue(s1.isSat());
    assertTrue(s1.getBool("directory.exists()"));
    assertFalse(s1.getBool("directory.isDirectory()"));

    // path 2: throw when exists() && isDirectory() && listFiles() == null
    SolverResult s2 = sols.get(2);
    assertTrue(s2.isSat());
    assertTrue(s2.getBool("directory.exists()"));
    assertTrue(s2.getBool("directory.isDirectory()"));
  }

  // cleanDirectory's only throw fires when verifiedListFiles RETURNED normally, so the
  // model must show exists()=true, isDirectory()=true. Regression: identity-keyed Z3 caches
  // gave each callee throw-path's directory.exists() its own bool const, so the constraints
  // never tied them together and Z3 freely set the surfaced const to false.
  @Test
  public void cleanDirectoryPreconditionsReflectCalleeReturning() {
    String methodSignature =
        "<br.unb.cic.witup.samples.File: void cleanDirectory(java.io.File)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    SolverResult sol = analysis.solutions().getFirst();

    assertTrue(sol.isSat(), "expected SAT for cleanDirectory's throw path");
    assertTrue(
        sol.getBool("directory.exists()"),
        "verifiedListFiles must have returned normally → exists() must be true");
    assertTrue(
        sol.getBool("directory.isDirectory()"),
        "verifiedListFiles must have returned normally → isDirectory() must be true");
  }
}
