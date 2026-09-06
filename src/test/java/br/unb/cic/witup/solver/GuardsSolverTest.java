package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.ThrowSiteKind;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GuardsSolverTest {

  private static final String PKG = "<br.unb.cic.witup.samples.Guards: ";

  private static List<SolverResult> solutionsOf(final String signature) {
    AnalysisResult analysis = TestAnalysisContext.getImplicitAnalyser().analyseMethod(signature);
    return analysis.solutions();
  }

  @Test
  public void aGuardedDereferenceIsRefuted() {
    // The whole point of the exercise. `if (name == null) return 0;` means the dereference below
    // is reached only when name is not null, so the null-pointer predicate cannot be satisfied.
    List<SolverResult> solutions = solutionsOf(PKG + "int guardedDeref(java.lang.String)>");

    assertEquals(1, solutions.size());
    assertFalse(
        solutions.getFirst().isSat(),
        "a guard that precludes the dereference must refute the row, not decorate it");
  }

  @Test
  public void anUnguardedDereferenceIsStillReachable() {
    List<SolverResult> solutions = solutionsOf(PKG + "int unguardedDeref(java.lang.String)>");

    assertEquals(1, solutions.size());
    assertTrue(solutions.getFirst().isSat(), "nothing rules this one out");
  }

  @Test
  public void rebindingAfterAGuardStaysReachable() {
    // Soundness. The guard constrains the parameter; the dereference is of maybeNull()'s result.
    // Reading the first as a statement about the second would refute a real exception.
    List<SolverResult> solutions = solutionsOf(PKG + "int guardThenReassign(java.lang.String)>");

    assertTrue(
        solutions.stream().anyMatch(SolverResult::isSat),
        "the NPE on the rebound value is real and must survive: " + solutions);
  }

  @Test
  public void repairingAGuardedValueRefutesBothWaysIn() {
    // `if (name == null) name = "";` — neither way in can dereference null. The path that skipped
    // the repair is refuted by the guard; the path that took it holds a string literal, which is
    // never null. Both need the site's operand to be read in the world of the path that reached
    // it, rather than looked up once for the site as a whole.
    List<SolverResult> solutions = solutionsOf(PKG + "int defaultOnNull(java.lang.String)>");

    assertTrue(
        solutions.stream().noneMatch(SolverResult::isSat),
        "no way of reaching this dereference can pass it null: " + solutions);
  }

  @Test
  public void aCallersGuardRefutesTheCalleesException() {
    // The guard is in the caller, the dereference in the callee. Neither half rules it out alone,
    // so this only falls if the caller's condition travels with the callee's predicate — after
    // the callee's formal has been replaced by the caller's actual.
    String sig = PKG + "int guardedCall(java.lang.String)>";
    List<SolverResult> solutions = TestAnalysisContext.solveObservablePaths(sig);

    assertFalse(solutions.isEmpty(), "the callee's NPE must reach the caller at all");
    assertTrue(
        solutions.stream().noneMatch(SolverResult::isSat),
        "the caller never passes null, so the callee's dereference cannot fail: " + solutions);
  }

  @Test
  public void anEarlierDereferenceRulesOutALaterOne() {
    // `name.length()` can fail on a null name, and that row must stand. But reaching the call to
    // calleeDeref means that dereference returned, which it could not have done on a null value —
    // so the exception propagated out of the callee cannot happen.
    String sig = PKG + "int derefThenPass(java.lang.String)>";
    List<ExceptionPath> paths = TestAnalysisContext.getImplicitWalker().observablePaths(sig);
    List<SolverResult> solutions = TestAnalysisContext.solveObservablePaths(sig);

    assertEquals(paths.size(), solutions.size(), "one verdict per observable path");
    Map<ThrowSiteKind, Boolean> satByKind = new EnumMap<>(ThrowSiteKind.class);
    for (int i = 0; i < paths.size(); i++) {
      satByKind.merge(paths.get(i).getThrowSiteKind(), solutions.get(i).isSat(), (a, b) -> a || b);
    }

    assertEquals(
        Boolean.TRUE,
        satByKind.get(ThrowSiteKind.IMPLICIT),
        "the first dereference is genuinely unguarded: " + solutions);
    assertEquals(
        Boolean.FALSE,
        satByKind.get(ThrowSiteKind.CALLEE_PROPAGATED),
        "the second cannot fail once the first has succeeded: " + solutions);
  }

  @Test
  public void aMergeDoesNotRefuteWhatItCannotRuleOut() {
    // Both branches rejoin before the dereference and neither says anything about `name`.
    List<SolverResult> solutions =
        solutionsOf(PKG + "int joinedBranches(java.lang.String,boolean)>");

    assertTrue(
        solutions.stream().anyMatch(SolverResult::isSat),
        "nothing here precludes a null name: " + solutions);
  }
}
