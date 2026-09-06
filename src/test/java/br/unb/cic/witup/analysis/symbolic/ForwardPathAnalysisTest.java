package br.unb.cic.witup.analysis.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.graph.ImplicitNpeReceiverSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ForwardPathAnalysisTest {

  private static final String PKG = "<br.unb.cic.witup.samples.Guards: ";

  // Every test states the budgets it depends on, so none of them silently pins a default.
  private static final int LARGE_CAP = 64;

  private static WITUpGraph graphOf(final String signature) {
    return TestAnalysisContext.getAnalyser().analyseMethod(signature).graph();
  }

  // The receiver of the sample's single dereference — the node Stage 4 will ask about.
  private static WITUpNode derefSiteOf(final WITUpGraph cpg, final String receiver) {
    List<ImplicitNpeReceiverSite> sites =
        cpg.getImplicitNpeReceiverSites().stream()
            .filter(s -> receiver.equals(s.receiver().getName()))
            .toList();
    assertEquals(1, sites.size(), "sample must have exactly one dereference of " + receiver);
    return sites.getFirst().node();
  }

  private static List<PathFact> factsAtDeref(final String signature, final String receiver) {
    return factsAtDeref(signature, receiver, LARGE_CAP, 1);
  }

  private static List<PathFact> factsAtDeref(
      final String signature, final String receiver, final int maxFacts, final int maxUnrolls) {
    WITUpGraph cpg = graphOf(signature);
    WITUpNode site = derefSiteOf(cpg, receiver);
    return ForwardPathAnalysis.analyseMethodPaths(cpg, Set.of(site), maxFacts, maxUnrolls)
        .factsAt(site);
  }

  private static Set<String> predicatesOf(final List<PathFact> facts) {
    return facts.stream().map(f -> f.pc().toList().toString()).collect(Collectors.toSet());
  }

  @Test
  public void guardReachingASiteRidesOnItsFact() {
    List<PathFact> facts = factsAtDeref(PKG + "int guardedDeref(java.lang.String)>", "name");

    assertEquals(1, facts.size(), "only the non-null branch reaches the dereference");
    assertEquals(1, facts.getFirst().pc().length());
    assertTrue(
        facts.getFirst().pc().toList().toString().contains("name"),
        "the guard must be about the guarded value: " + facts.getFirst().pc());
  }

  @Test
  public void anUnguardedSiteCarriesAnEmptyPathCondition() {
    List<PathFact> facts = factsAtDeref(PKG + "int unguardedDeref(java.lang.String)>", "name");

    assertEquals(1, facts.size());
    assertEquals(0, facts.getFirst().pc().length(), "nothing had to hold to get here");
  }

  @Test
  public void parametersAreBoundInTheEnvironment() {
    List<PathFact> facts = factsAtDeref(PKG + "int unguardedDeref(java.lang.String)>", "name");

    SymExpr bound = facts.getFirst().env().get("name");
    assertInstanceOf(
        SymParamRef.class,
        bound,
        "a formal must resolve to its index so callers can substitute it");
  }

  @Test
  public void aMergeKeepsBothIncomingFacts() {
    List<PathFact> facts =
        factsAtDeref(PKG + "int joinedBranches(java.lang.String,boolean)>", "name");

    assertEquals(2, facts.size(), "union at merges: one fact per branch, uncapped in this stage");
    assertEquals(2, predicatesOf(facts).size(), "and they must differ, or the merge said nothing");
  }

  @Test
  public void independentGuardsAccumulate() {
    List<PathFact> facts = factsAtDeref(PKG + "char nestedGuards(java.lang.String,int)>", "name");

    assertEquals(1, facts.size());
    assertEquals(
        2, facts.getFirst().pc().length(), "both early returns guard the access: " + facts);
  }

  @Test
  public void rebindingAValueDetachesItFromTheGuardAboutIt() {
    List<PathFact> facts = factsAtDeref(PKG + "int guardThenReassign(java.lang.String)>", "name");

    assertEquals(2, facts.size(), "guarded and repaired paths both reach the dereference");

    List<SymExpr> bindings = facts.stream().map(f -> f.env().get("name")).toList();
    assertTrue(
        bindings.stream().anyMatch(b -> b instanceof SymParamRef),
        "the path that skipped the repair still holds the parameter: " + bindings);
    assertTrue(
        bindings.stream().anyMatch(b -> b instanceof SymStaticInvoke),
        "the repaired path holds maybeNull()'s result, not the parameter: " + bindings);
  }

  @Test
  public void aLoopTerminatesWithinItsUnrollBudget() {
    WITUpGraph cpg = graphOf(PKG + "int totalLength(java.lang.String[])>");
    Set<WITUpNode> sites =
        cpg.getImplicitNpeReceiverSites().stream()
            .map(ImplicitNpeReceiverSite::node)
            .collect(Collectors.toSet());

    // Reaching this line at all is most of the assertion: without a loop bound the worklist
    // would keep minting longer path conditions round the cycle and never drain.
    int unrollBudget = 2;
    PathConditionIndex index =
        ForwardPathAnalysis.analyseMethodPaths(cpg, sites, LARGE_CAP, unrollBudget);
    List<PathFact> facts = sites.stream().flatMap(s -> index.factsAt(s).stream()).toList();

    assertFalse(facts.isEmpty(), "the loop body is reachable");
    int components = cpg.sccIndex().topologicalSccs().size();
    for (PathFact fact : facts) {
      for (int component = 0; component < components; component++) {
        assertTrue(
            fact.unrollsOf(component) <= unrollBudget,
            "no fact may exceed the unroll budget in any component: " + fact);
      }
    }
  }

  @Test
  public void belowTheCapEveryWayOfArrivingIsKeptApart() {
    List<PathFact> facts =
        factsAtDeref(PKG + "int guardedThenBranching(java.lang.String,boolean)>", "name", 2, 1);

    assertEquals(2, facts.size(), "two ways in, and room for both");
    for (PathFact fact : facts) {
      assertFalse(fact.isWidened(), "nothing was given up: " + fact);
    }
  }

  @Test
  public void aboveTheCapOnlyWhatTheyAgreeOnSurvives() {
    List<PathFact> facts =
        factsAtDeref(PKG + "int guardedThenBranching(java.lang.String,boolean)>", "name", 1, 1);

    assertEquals(1, facts.size(), "the cap forced the two ways in together");
    PathFact collapsed = facts.getFirst();
    assertTrue(collapsed.isWidened());
    assertEquals(
        1,
        collapsed.pc().length(),
        "the shared guard survives and the branch decision does not: " + collapsed.pc());
    assertTrue(
        collapsed.pc().toList().toString().contains("name"),
        "and the surviving one is the guard: " + collapsed.pc());
  }

  @Test
  public void collapsingKeepsAgreedBindingsAndOpaquesTheRest() {
    List<PathFact> facts =
        factsAtDeref(PKG + "int guardedThenBranching(java.lang.String,boolean)>", "name", 1, 1);
    PathFact collapsed = facts.getFirst();

    assertInstanceOf(
        SymParamRef.class,
        collapsed.env().get("name"),
        "every way in agreed what `name` was, so it is still the parameter");

    SymExpr n = collapsed.env().get("n");
    assertInstanceOf(SymVar.class, n, "the branches disagreed about `n`: " + n);
    assertNotEquals(
        "n",
        n.toString(),
        "an opaque value must not reuse the name a recorded decision may already mention");
  }

  @Test
  public void collapsingIsReproducibleAcrossRuns() {
    // The generated name has to come from the merge point, not a counter. A counter would make
    // two runs disagree, and — worse — would stop the pass ever deciding it had finished.
    String sig = PKG + "int guardedThenBranching(java.lang.String,boolean)>";
    assertEquals(
        factsAtDeref(sig, "name", 1, 1).toString(), factsAtDeref(sig, "name", 1, 1).toString());
  }
}
