package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CatchesGraphTest {
  @Test
  public void simpleCatchGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleCatch(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    // simpleCatch contains exactly one throw stmt (`throw new IllegalArgumentException`).
    assertEquals(1, throwNodes.size());
  }

  @Test
  public void simpleRethrowGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleRethrow(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    // simpleRethrow contains exactly one throw stmt (`throw exception`).
    assertEquals(1, throwNodes.size());

    // `exception` is written twice — `= null` at the top and `= t` inside the handler — so the
    // DDG walk from the throw operand hits a join, not a linear copy chain. That is what keeps
    // a genuine rethrow-through-a-variable distinguishable from javac's synthetic one.
    assertFalse(
        cpg.isSyntheticCatchAllRethrow((ThrowStatementNode) throwNodes.getFirst()),
        "a source-level rethrow through a variable must not be mistaken for a synthetic one");
  }

  @Test
  public void tryFinallyGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void tryFinally(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    // tryFinally authors no throw of its own; the only athrow in the body is the one javac
    // synthesises for the finally handler.
    assertEquals(1, throwNodes.size());
    assertTrue(
        cpg.isSyntheticCatchAllRethrow((ThrowStatementNode) throwNodes.getFirst()),
        "the finally handler's rethrow must be recognised as synthetic");
  }
}
