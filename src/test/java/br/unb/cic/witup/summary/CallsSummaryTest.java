package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ThrowSiteKind;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CallsSummaryTest {
  @Test
  public void unguardedCalleeThrowSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void unguardedCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    // The unguarded call to calleeMayThrow surfaces its IllegalArgumentException as a
    // CALLEE_PROPAGATED path. Predicate inherits calleeMayThrow's `(x >= 0, false)` shape
    // (the if-condition that, when false, reaches the throw); after formals→actuals
    // substitution, x resolves to caller's own x parameter — same name, same toString.
    assertEquals(1, summary.exceptionPaths().size());
    ExceptionPath ep = summary.exceptionPaths().getFirst();
    assertEquals("java.lang.IllegalArgumentException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.CALLEE_PROPAGATED, ep.getThrowSiteKind());
    assertEquals(
        List.of("<br.unb.cic.witup.samples.Calls: void calleeMayThrow(int)>"), ep.getProvenance());

    List<SymbolicConstraint> path0 = ep.getConstraints();
    assertEquals(1, path0.size());
    assertFalse(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("x >= 0"));
  }

  @Test
  public void caughtCalleeThrowSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Calls: void caughtCalleeThrow(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    // calleeMayThrow raises IllegalArgumentException; the surrounding catch handler catches
    // exactly that type, so the rollup absorbs it. No CALLEE_PROPAGATED path emitted.
    assertTrue(summary.exceptionPaths().isEmpty());
  }
}
