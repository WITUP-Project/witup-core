package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class CallsSummaryTest {
  // The MethodSummary now carries only this method's *own* local throws (DIRECT_ATHROW /
  // RETHROW / IMPLICIT). Transitive observable flow — propagated callee exceptions filtered
  // through this method's catch handlers — is computed by ExceptionFlowWalker on demand.
  // The walker has its own tests.

  @Test
  public void unguardedCalleeThrowSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void unguardedCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    // unguardedCalleeThrow has no athrow of its own — the IAE escape is observable via
    // ExceptionFlowWalker.observablePaths, not via the local summary.
    assertTrue(summary.exceptionPaths().isEmpty());
  }

  @Test
  public void caughtCalleeThrowSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void caughtCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    // No own throws, and catch absorption happens at the walker level — local summary empty.
    assertTrue(summary.exceptionPaths().isEmpty());
  }
}
