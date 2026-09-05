package br.unb.cic.witup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Within-method dedup of the summaries output, driven by the walker's real observable paths over
 * the Calls samples.
 *
 * <p>The ESU benchmark lists one row per (exception, predicate) and does not care which call chain
 * reached it, so provenance must not be part of a flow's identity. throwSiteKind must be, because
 * it names the mechanism the coverage breakdown reports.
 */
public class SummaryRowBuilderTest {

  private static final String ARTIFACT = "witup-test-jar.jar";

  private static List<ExceptionPath> pathsOf(final String methodSignature) {
    return TestAnalysisContext.getImplicitWalker().observablePaths(methodSignature);
  }

  private static String constraintsOf(final ExceptionPath ep) {
    return ep.getConstraints().stream()
        .map(SymbolicConstraint::toString)
        .collect(Collectors.joining(" & "));
  }

  @Test
  public void samePredicateViaDifferentChainsCollapsesToOneRow() {
    // sameNpeViaTwoChains reaches the same `s == null` NPE directly through derefLength and one
    // frame deeper through derefIndirect -> derefLength. Simplified shape of
    // FilenameUtils.isExtension, which reaches its `fileName == null` NPE through
    // indexOfExtension, through getExtension, and through indexOfExtension ->
    // getAdsCriticalOffset. One observable flow, several chains.
    String sig = "<br.unb.cic.witup.samples.Calls: int sameNpeViaTwoChains(java.lang.String)>";
    List<ExceptionPath> paths = pathsOf(sig);

    // Guard the premise: if the analyser ever stops producing collidable paths here, this test
    // would pass vacuously.
    assertEquals(2, paths.size(), "sample must yield two paths for the dedup to have work to do");
    assertEquals(
        1,
        paths.stream().map(SummaryRowBuilderTest::constraintsOf).collect(Collectors.toSet()).size(),
        "both paths must carry the identical predicate");
    assertNotEquals(
        paths.get(0).getProvenance(),
        paths.get(1).getProvenance(),
        "and must differ only in the chain that reached it");

    List<Map<String, Object>> rows =
        SummaryRowBuilder.rowsForMethod(ARTIFACT, sig, paths, Map.of());

    assertEquals(1, rows.size(), "one observable flow, so one row");
    assertEquals(
        List.of(paths.get(0).getProvenance(), paths.get(1).getProvenance()),
        rows.getFirst().get("provenances"),
        "every collapsed chain must survive in provenances");
    assertEquals(
        paths.get(0).getProvenance(),
        rows.getFirst().get("provenance"),
        "provenance still names the emitted path's chain, so provenance[0] lookups keep working");
  }

  @Test
  public void throwSiteKindKeepsIdenticalPredicatesApart() {
    // ownAndCalleeNpe raises `s == null` twice: once by dereferencing s itself (IMPLICIT) and
    // once inside derefLength (CALLEE_PROPAGATED). Same exception, same predicate — only the
    // mechanism differs, and that difference is what the coverage breakdown counts.
    String sig = "<br.unb.cic.witup.samples.Calls: int ownAndCalleeNpe(java.lang.String)>";
    List<ExceptionPath> paths = pathsOf(sig);

    assertEquals(2, paths.size());
    assertEquals(
        1,
        paths.stream().map(SummaryRowBuilderTest::constraintsOf).collect(Collectors.toSet()).size(),
        "the two paths must be indistinguishable except by kind for this test to bite");
    assertEquals(
        1,
        paths.stream()
            .map(ExceptionPath::getExceptionQualifiedName)
            .collect(Collectors.toSet())
            .size());

    List<Map<String, Object>> rows =
        SummaryRowBuilder.rowsForMethod(ARTIFACT, sig, paths, Map.of());

    assertEquals(2, rows.size(), "throwSiteKind carries identity, so these must not collapse");
    assertEquals(
        Set.of("IMPLICIT", "CALLEE_PROPAGATED"),
        rows.stream().map(r -> r.get("throwSiteKind")).collect(Collectors.toSet()));
  }

  @Test
  public void parameterUnderALengthExpressionIsSubstituted() {
    // throwIfEmpty's predicate is `a.length == 0`, so the parameter sits under a SymLength.
    // Composed into callThrowIfEmpty it must talk about the caller's `items`;
    String sig = "<br.unb.cic.witup.samples.Calls: void callThrowIfEmpty(int[])>";
    List<ExceptionPath> paths = pathsOf(sig);
    assertEquals(1, paths.size());

    String predicate = constraintsOf(paths.getFirst());
    assertTrue(predicate.contains("items"), "expected the caller's array, got " + predicate);
    assertFalse(predicate.contains("(a)"), "callee's local leaked into the caller: " + predicate);
  }

  @Test
  public void implicitSitePredicateIsSubstitutedIntoTheCaller() {
    String sig = "<br.unb.cic.witup.samples.Calls: int callDerefWithOtherName(java.lang.String)>";
    List<ExceptionPath> paths = pathsOf(sig);
    assertEquals(1, paths.size());

    String predicate = constraintsOf(paths.getFirst());
    assertTrue(predicate.contains("zzz"), "expected the caller's parameter, got " + predicate);
    assertFalse(
        predicate.matches(".*\\bs\\b.*"),
        "callee's parameter leaked into the caller: " + predicate);
  }

  @Test
  public void receiverIsSubstitutedIntoAComposedPredicate() {
    // circleArea throws on `this.radius < 0`. Composed into the static areaOf(Math shape), the
    // receiver is passed as the last actual against formal index -1, so the predicate must be
    // about `shape`.
    String sig = "<br.unb.cic.witup.samples.Math: double areaOf(br.unb.cic.witup.samples.Math)>";
    List<ExceptionPath> paths = pathsOf(sig);

    // Two observable flows: dereferencing the receiver can raise NPE, and circleArea's own
    // RuntimeException. The composed one is what carries the substituted predicate.
    ExceptionPath composed =
        paths.stream()
            .filter(p -> "java.lang.RuntimeException".equals(p.getExceptionQualifiedName()))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("circleArea's throw must escape areaOf: " + paths));

    String predicate = constraintsOf(composed);
    assertTrue(predicate.contains("shape"), "expected the caller's receiver, got " + predicate);
    assertFalse(
        predicate.contains("this."), "callee's `this` leaked into a static caller: " + predicate);
  }

  @Test
  public void composedRowCarriesARealVerdictNotAPlaceholder() {
    // End to end: compose, solve, then build the row. alwaysOrdered's only observable flow is
    // infeasible after substitution, and the emitted row must say so.
    String sig = "<br.unb.cic.witup.samples.Calls: void alwaysOrdered(int)>";
    List<ExceptionPath> paths = pathsOf(sig);
    List<SolverResult> results = TestAnalysisContext.solveObservablePaths(sig);

    Map<String, String> statuses = new LinkedHashMap<>();
    for (SolverResult r : results) {
      statuses.put(r.getPathId(), r.getStatus().toString());
    }

    List<Map<String, Object>> rows =
        SummaryRowBuilder.rowsForMethod(ARTIFACT, sig, paths, statuses);

    assertEquals(1, rows.size());
    assertEquals(
        "CALLEE_PROPAGATED", rows.getFirst().get("throwSiteKind"), "still a composed flow");
    assertEquals(
        "UNSAT",
        rows.getFirst().get("solverStatus"),
        "composed flows now carry a solver verdict; the kind field records that it was composed");
  }

  @Test
  public void collapsedRowKeepsFirstOccurrenceIdentity() {
    // The walker emits own throws before callee-propagated ones, so the colliding NPE paths sit
    // at indices 1 and 2 behind the IAE at 0. We must keep index 1
    String sig =
        "<br.unb.cic.witup.samples.Calls: int ownThrowThenTwoChains(java.lang.String,int)>";
    List<ExceptionPath> paths = pathsOf(sig);
    assertEquals(3, paths.size());

    // Distinct verdicts per index, so the assertion can tell which one was picked up.
    Map<String, String> statuses =
        Map.of(sig + "#0", "SAT", sig + "#1", "UNSAT", sig + "#2", "MAYBE");

    List<Map<String, Object>> rows =
        SummaryRowBuilder.rowsForMethod(ARTIFACT, sig, paths, statuses);

    assertEquals(2, rows.size(), "the two NPE paths collapse, the IAE stays");
    Map<String, Object> collapsed =
        rows.stream()
            .filter(r -> "CALLEE_PROPAGATED".equals(r.get("throwSiteKind")))
            .findFirst()
            .orElseThrow();
    assertEquals(1, collapsed.get("pathIndex"), "index of the first occurrence, not 0 and not 2");
    assertEquals(sig + "#1", collapsed.get("pathId"));
    assertEquals("UNSAT", collapsed.get("solverStatus"), "verdict must follow the emitted pathId");
    assertTrue(
        ((List<?>) collapsed.get("provenances")).size() == 2, "both chains recorded on the row");
  }
}
