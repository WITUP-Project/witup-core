package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class ImplicitsSummaryTest {
  @Test
  public void receiverNpeSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int receiverNpe(java.lang.Object)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    ExceptionPath ep = summary.exceptionPaths().getFirst();
    assertEquals("java.lang.NullPointerException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());

    // Predicate: receiver is null. truthValue=true means `o == null` must hold.
    List<SymbolicConstraint> path0 = ep.getConstraints();
    assertEquals(1, path0.size());
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("o == null"));
  }

  @Test
  public void fieldNpeSummary() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int fieldNpe(br.unb.cic.witup.samples.Implicits$Box)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    ExceptionPath ep = summary.exceptionPaths().getFirst();
    assertEquals("java.lang.NullPointerException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());

    List<SymbolicConstraint> path0 = ep.getConstraints();
    assertEquals(1, path0.size());
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("b == null"));
  }

  @Test
  public void arrayDerefSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int arrayDeref(int[])>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    // path 0: NPE on the array base. collectImplicitNpePaths runs first.
    // path 1: AIOOBE on the index, with `arr != null` conjoined so the witness doesn't
    //         conflate with NPE (the JVM raises NPE before AIOOBE when both could fire).
    assertEquals(2, summary.exceptionPaths().size());

    ExceptionPath npe = summary.exceptionPaths().getFirst();
    assertEquals("java.lang.NullPointerException", npe.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, npe.getThrowSiteKind());
    assertEquals(List.of(), npe.getProvenance());

    List<SymbolicConstraint> npePath = npe.getConstraints();
    assertEquals(1, npePath.size());
    assertTrue(npePath.getFirst().truthValue());
    assertTrue(npePath.getFirst().symExpr().toString().contains("arr == null"));

    ExceptionPath aioobe = summary.exceptionPaths().get(1);
    assertEquals("java.lang.ArrayIndexOutOfBoundsException", aioobe.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, aioobe.getThrowSiteKind());
    assertEquals(List.of(), aioobe.getProvenance());

    List<SymbolicConstraint> aioobePath = aioobe.getConstraints();
    assertEquals(1, aioobePath.size());
    assertTrue(aioobePath.getFirst().truthValue());
    String aioobeExpr = aioobePath.getFirst().symExpr().toString();
    assertTrue(aioobeExpr.contains("arr != null"), "AIOOBE predicate must include arr != null");
    assertTrue(aioobeExpr.contains("arr.length"), "AIOOBE predicate must include arr.length");
  }

  @Test
  public void negativeArraySizeSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int[] negativeArraySize(int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    ExceptionPath ep = summary.exceptionPaths().getFirst();
    assertEquals("java.lang.NegativeArraySizeException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());

    List<SymbolicConstraint> path0 = ep.getConstraints();
    assertEquals(1, path0.size());
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("n < 0"));
  }

  @Test
  public void divByZeroSummary() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int divByZero(int,int)>";
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(methodSignature);
    MethodSummary summary = analysis.summary();
    assertNotNull(summary);
    assertEquals(methodSignature, summary.methodSignature());

    assertEquals(1, summary.exceptionPaths().size());
    ExceptionPath ep = summary.exceptionPaths().getFirst();
    assertEquals("java.lang.ArithmeticException", ep.getExceptionQualifiedName());
    assertEquals(ThrowSiteKind.IMPLICIT, ep.getThrowSiteKind());
    assertEquals(List.of(), ep.getProvenance());

    List<SymbolicConstraint> path0 = ep.getConstraints();
    assertEquals(1, path0.size());
    assertTrue(path0.getFirst().truthValue());
    assertTrue(path0.getFirst().symExpr().toString().contains("b == 0"));
  }
}
