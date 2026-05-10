package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ThrowSiteKind;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CatchesSummaryTest {
  @Test
  public void simpleCatchSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleCatch(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertFalse(summary.exceptionPaths().isEmpty(), "expected at least one exception path");
    for (ExceptionPath ep : summary.exceptionPaths()) {
      assertEquals(
          "java.lang.IllegalArgumentException",
          ep.getExceptionQualifiedName(),
          "simpleCatch only authors IAE");
      assertEquals(
          ThrowSiteKind.DIRECT_ATHROW,
          ep.getThrowSiteKind(),
          "simpleCatch's `throw new IAE()` is a direct athrow, not a rethrow");
      assertEquals(
          List.of(),
          ep.getProvenance(),
          "no callee-summary integration yet — provenance is empty until step 3");
    }
  }

  @Test
  public void simpleRethrowSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleRethrow(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertFalse(summary.exceptionPaths().isEmpty(), "expected at least one exception path");
    for (ExceptionPath ep : summary.exceptionPaths()) {
      assertEquals(
          ThrowSiteKind.RETHROW,
          ep.getThrowSiteKind(),
          "throw operand traces back to a caught exception ref, so this is a rethrow");
      assertEquals(
          List.of(),
          ep.getProvenance(),
          "no callee-summary integration yet — provenance is empty until step 3");
    }
  }

  @Test
  public void loopCatchSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void loopCatch(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    System.out.println("=== loopCatch ===");
    dump(summary);
  }

  private static void dump(final MethodSummary summary) {
    System.out.println("methodSignature=" + summary.methodSignature());
    System.out.println("formalParams.size=" + summary.formalParams().size());
    for (int i = 0; i < summary.formalParams().size(); i++) {
      var p = summary.formalParams().get(i);
      System.out.println("  formal[" + i + "] type=" + p.getParamType() + " kind=" + p.getKind());
    }
    System.out.println("guardedReturn.size=" + summary.guardedReturn().size());
    System.out.println("exceptionPaths.size=" + summary.exceptionPaths().size());
    List<ExceptionPath> paths = summary.exceptionPaths();
    for (int i = 0; i < paths.size(); i++) {
      ExceptionPath ep = paths.get(i);
      System.out.println(
          "  path["
              + i
              + "] exception="
              + ep.getExceptionQualifiedName()
              + " constraints="
              + ep.getConstraints().size());
      List<SymbolicConstraint> cs = ep.getConstraints();
      for (int j = 0; j < cs.size(); j++) {
        SymbolicConstraint c = cs.get(j);
        System.out.println(
            "    ["
                + j
                + "] truth="
                + c.truthValue()
                + " ("
                + c.symExpr().getClass().getSimpleName()
                + ") "
                + c.symExpr());
      }
    }
  }
}
