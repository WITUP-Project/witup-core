package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ThrowSiteKind;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ImplicitsSummaryTest {
  @Test
  public void receiverNpeSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int receiverNpe(java.lang.Object)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    List<ExceptionPath> paths = summary.exceptionPaths();
    // No explicit `athrow` in receiverNpe, so the only ExceptionPath is the implicit NPE
    // synthesised at the `o.hashCode()` call site.
    assertEquals(1, paths.size());
    ExceptionPath ep = paths.get(0);
    assertEquals("java.lang.NullPointerException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());
  }

  @Test
  public void fieldNpeSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int fieldNpe(br.unb.cic.witup.samples.Implicits$Box)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    List<ExceptionPath> paths = summary.exceptionPaths();
    // No explicit `athrow` in fieldNpe, so the only ExceptionPath is the implicit NPE
    // synthesised at the `b.x` field read.
    assertEquals(1, paths.size());
    ExceptionPath ep = paths.get(0);
    assertEquals("java.lang.NullPointerException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());
  }

  @Test
  public void arrayDerefSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int arrayDeref(int[])>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    // path 0: NPE on the array base. collectImplicitNpePaths runs first.
    // path 1: AIOOBE on the index, with `arr != null` conjoined so the witness doesn't
    //         conflate with NPE (the JVM raises NPE before AIOOBE when both could fire).
    List<ExceptionPath> paths = summary.exceptionPaths();
    assertEquals(2, paths.size());

    ExceptionPath npe = paths.get(0);
    assertEquals("java.lang.NullPointerException", npe.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, npe.getThrowSiteKind());
    assertEquals(List.of(), npe.getProvenance());

    ExceptionPath aioobe = paths.get(1);
    assertEquals("java.lang.ArrayIndexOutOfBoundsException", aioobe.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, aioobe.getThrowSiteKind());
    assertEquals(List.of(), aioobe.getProvenance());
  }

  @Test
  public void negativeArraySizeSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int[] negativeArraySize(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    // Single implicit path: `new int[n]` raises NegativeArraySizeException when n < 0.
    List<ExceptionPath> paths = summary.exceptionPaths();
    assertEquals(1, paths.size());
    ExceptionPath ep = paths.get(0);
    assertEquals("java.lang.NegativeArraySizeException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());
  }

  @Test
  public void divByZeroSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int divByZero(int,int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);

    // Single implicit path: `a / b` raises ArithmeticException when b == 0.
    List<ExceptionPath> paths = summary.exceptionPaths();
    assertEquals(1, paths.size());
    ExceptionPath ep = paths.get(0);
    assertEquals("java.lang.ArithmeticException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());
  }
}
