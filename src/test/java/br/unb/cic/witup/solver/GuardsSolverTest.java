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
  public void anArrayLengthIsNeverNegative() {
    // The JLS guarantees it, so the throw is unreachable. Nothing in the encoding says so on its
    // own: a length is translated to a plain integer constant with no tie to the array it measures.
    // Only the IllegalStateException is impossible. Reading `xs.length` is itself a dereference,
    // so a null argument raises NPE here — a different, genuine flow.
    AnalysisResult analysis =
        TestAnalysisContext.getImplicitAnalyser()
            .analyseMethod(PKG + "int impossibleNegativeLength(int[])>");
    List<ExceptionPath> paths = analysis.summary().exceptionPaths();
    List<SolverResult> solutions = analysis.solutions();
    assertEquals(paths.size(), solutions.size(), "one verdict per path");

    boolean anyStateExceptionSat = false;
    for (int i = 0; i < paths.size(); i++) {
      if ("java.lang.IllegalStateException".equals(paths.get(i).getExceptionQualifiedName())) {
        anyStateExceptionSat |= solutions.get(i).isSat();
      }
    }
    assertFalse(anyStateExceptionSat, "no array can have a negative length: " + solutions);
  }

  @Test
  public void aLengthReadCanThrowButProtectsWhatFollows() {
    List<SolverResult> solutions = solutionsOf(PKG + "int lengthThenIndex(int[])>");

    assertTrue(
        solutions.stream().anyMatch(SolverResult::isSat),
        "reading the length of a null array is a real exception: " + solutions);
    assertTrue(
        solutions.stream().anyMatch(s -> !s.isSat()),
        "and having read it, the access below cannot fail the same way: " + solutions);
  }

  private static void assertNoSatMentioning(final String signature, final String fragment) {
    AnalysisResult analysis = TestAnalysisContext.getImplicitAnalyser().analyseMethod(signature);
    List<ExceptionPath> paths = analysis.summary().exceptionPaths();
    List<SolverResult> solutions = analysis.solutions();
    assertEquals(paths.size(), solutions.size(), "one verdict per path");
    for (int i = 0; i < paths.size(); i++) {
      if (paths.get(i).getConstraints().toString().contains(fragment)) {
        assertFalse(
            solutions.get(i).isSat(),
            "predicate mentioning " + fragment + " must be refuted: " + paths.get(i));
      }
    }
  }

  @Test
  public void getClassNeverReturnsNull() {
    assertNoSatMentioning(PKG + "int classNameLength(java.lang.Object)>", "getClass() == null");
  }

  @Test
  public void aCastDoesNotChangeNullness() {
    assertNoSatMentioning(PKG + "int castThenDeref(java.lang.Object)>", "== null");
  }

  @Test
  public void aPositiveInstanceOfProvesNonNull() {
    assertNoSatMentioning(PKG + "int instanceOfThenDeref(java.lang.Object)>", "== null");
  }

  @Test
  public void aContractHoldsForTheTypeThatDeclaresIt() {
    // String is final, so String.toCharArray() cannot be overridden and cannot return null.
    assertNoSatMentioning(PKG + "int charsOfString(java.lang.String)>", "toCharArray() == null");
  }

  @Test
  public void aContractDoesNotLeakToASameNamedMethodElsewhere() {
    // The pair that matters. Impostor.toCharArray() genuinely returns null, and a contract keyed
    // on the method name rather than the declaring type would refute a real exception here.
    List<SolverResult> solutions =
        solutionsOf(PKG + "int charsOfImpostor(br.unb.cic.witup.samples.Guards$Impostor)>");

    assertTrue(
        solutions.stream().anyMatch(SolverResult::isSat),
        "a same-named method on another type keeps its own behaviour: " + solutions);
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
