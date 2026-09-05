package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

public class CatchesSolverTest {

  @Test
  public void simpleCatchSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleCatch(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);

    // path 0: catch fired, post-catch throw fires. The substitution chain resolves
    // `exception` to the catch-handler's local (a SootUp stack temp like `stack_N`),
    // so we anchor on the path-level catch flag rather than a slot-number-dependent
    // `*_is_null` key.
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(
        sol0.getBool("caught_java_lang_Throwable"),
        "catch-fired path must assert the throwable was caught");
    assertFalse(
        sol0.getBool("caught_java_lang_Throwable_is_null"),
        "catch-fired path requires the caught throwable to be non-null");
  }

  @Test
  public void simpleRethrowSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleRethrow(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);

    // Same path-condition shape as simpleCatch: the symbolic constraints depend on the
    // catch-fired flag and the post-catch null-check, not on whether the throw operand
    // is freshly allocated or rebound from the caught reference. Rethrow vs direct
    // athrow shows up in classifyThrowSite, not in the solver's SAT/UNSAT verdict.
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(
        sol0.getBool("caught_java_lang_Throwable"),
        "catch-fired path must assert the throwable was caught");
    assertFalse(
        sol0.getBool("caught_java_lang_Throwable_is_null"),
        "catch-fired path requires the caught throwable to be non-null");
  }

  @Test
  public void tryFinallySolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void tryFinally(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);

    // Suppressing the synthetic finally rethrow leaves the method with no own exception path,
    // so the solver has nothing to check. This is the end of the chain the graph and summary
    // tests start: predicate recognises it, summariser drops it, solver never sees it.
    assertTrue(
        analysis.solutions().isEmpty(),
        "no exception paths means no solver work for the synthetic rethrow");
  }

  @Test
  public void loopCatchSolution() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void loopCatch(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);

    // path 0: loop entered, catch fired on the iteration, then post-loop throw.
    // Per-iteration symbolization makes loop-entry (i#0 < xs.length) and loop-exit
    // (i#1 >= xs.length) consistent, so this path is SAT (previously UNSAT-bogus).
    SolverResult sol0 = analysis.solutions().getFirst();
    assertTrue(sol0.isSat());
    assertTrue(
        sol0.getBool("caught_java_lang_Throwable"),
        "catch-fired path must assert the throwable was caught");
    assertFalse(
        sol0.getBool("caught_java_lang_Throwable_is_null"),
        "catch-fired path requires the caught throwable to be non-null");
  }
}
