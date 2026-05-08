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

public class FileSummaryTest {

  @Test
  public void verifiedListFilesSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.File: java.io.File[] verifiedListFiles(java.io.File)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(3, summary.exceptionPaths().size());

    // path 0: !directory.exists() throws IAE
    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());
    assertFalse(path0.isEmpty(), "path 0 expected at least one constraint, got " + path0);
    assertTrue(
        path0.toString().contains("directory.exists()"),
        "path 0 should mention directory.exists(), got " + path0);

    // path 1: directory.exists() && !directory.isDirectory() throws IAE
    List<SymbolicConstraint> path1 = summary.exceptionPaths().get(1).getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().get(1).getExceptionQualifiedName());
    assertTrue(
        path1.toString().contains("directory.isDirectory()"),
        "path 1 should mention directory.isDirectory(), got " + path1);

    // path 2: exists && isDirectory && listFiles() == null throws ISE
    List<SymbolicConstraint> path2 = summary.exceptionPaths().get(2).getConstraints();
    assertEquals(
        "java.lang.IllegalStateException",
        summary.exceptionPaths().get(2).getExceptionQualifiedName());
    assertTrue(
        path2.toString().contains("listFiles()"),
        "path 2 should mention listFiles(), got " + path2);
  }

  @Test
  public void cleanDirectorySummary() {
    String methodSignature = "<br.unb.cic.witup.samples.File: void cleanDirectory(java.io.File)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(1, summary.exceptionPaths().size());

    List<SymbolicConstraint> path0 = summary.exceptionPaths().getFirst().getConstraints();
    assertEquals(
        "java.lang.IllegalArgumentException",
        summary.exceptionPaths().getFirst().getExceptionQualifiedName());

    // Sanity: the path should reference each of the three callee-throw conditions
    // (so the caller has actually pulled the callee summary in).
    String dump = path0.toString();
    assertTrue(
        dump.contains("directory.exists()"), "expected directory.exists() in path, got " + dump);
    assertTrue(
        dump.contains("directory.isDirectory()"),
        "expected directory.isDirectory() in path, got " + dump);
    assertTrue(dump.contains("listFiles()"), "expected listFiles() in path, got " + dump);
  }
}
