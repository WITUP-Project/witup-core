package br.unb.cic.witup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
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

    List<Map<String, Object>> rows = SummaryRowBuilder.rowsForMethod(ARTIFACT, sig, paths, Map.of());

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

    List<Map<String, Object>> rows = SummaryRowBuilder.rowsForMethod(ARTIFACT, sig, paths, Map.of());

    assertEquals(2, rows.size(), "throwSiteKind carries identity, so these must not collapse");
    assertEquals(
        Set.of("IMPLICIT", "CALLEE_PROPAGATED"),
        rows.stream().map(r -> r.get("throwSiteKind")).collect(Collectors.toSet()));
  }

  @Test
  public void collapsedRowKeepsFirstOccurrenceIdentity() {
    // The walker emits own throws before callee-propagated ones, so the colliding NPE paths sit
    // at indices 1 and 2 behind the IAE at 0. The survivor must keep index 1: witup-results keys
    // the solver's per-path verdicts by pathId, so taking the wrong occurrence would attach the
    // wrong verdict to the row.
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
