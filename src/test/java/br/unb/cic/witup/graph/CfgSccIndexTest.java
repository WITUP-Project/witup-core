package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.graph.CfgSccIndex;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.CFGEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CfgSccIndexTest {

  private static WITUpGraph graphOf(final String methodSignature) {
    return TestAnalysisContext.getAnalyser().analyseMethod(methodSignature).graph();
  }

  private static List<WITUpEdge> cfgEdgesOf(final WITUpGraph cpg) {
    return cpg.edgeSet().stream().filter(e -> e instanceof CFGEdge).toList();
  }

  @Test
  public void loopFreeMethodHasNoIntraComponentEdges() {
    WITUpGraph cpg = graphOf("<br.unb.cic.witup.samples.Array: int checkLength(int[])>");
    CfgSccIndex index = cpg.sccIndex();

    assertTrue(index.topologicalSccs().size() > 1, "method must decompose into several components");
    assertFalse(cfgEdgesOf(cpg).isEmpty(), "method must have CFG edges to classify");

    for (List<WITUpNode> component : index.topologicalSccs()) {
      assertEquals(1, component.size(), "loop-free method must decompose into singletons");
    }
    for (WITUpEdge edge : cfgEdgesOf(cpg)) {
      assertFalse(
          index.isIntraScc(edge),
          "no edge in a loop-free method closes a cycle: "
              + edge.getSource()
              + " -> "
              + edge.getTarget());
    }
  }

  @Test
  public void loopBodyFormsOneNonTrivialComponent() {
    WITUpGraph cpg = graphOf("<br.unb.cic.witup.samples.Array: int sumUntilZero(int[])>");
    CfgSccIndex index = cpg.sccIndex();

    List<List<WITUpNode>> nonTrivial =
        index.topologicalSccs().stream().filter(c -> c.size() > 1).toList();
    assertEquals(1, nonTrivial.size(), "exactly one loop, so exactly one non-trivial component");

    long intra = cfgEdgesOf(cpg).stream().filter(index::isIntraScc).count();
    assertTrue(intra > 0, "the loop must contribute at least one cycle-closing edge");

    List<WITUpNode> loop = nonTrivial.getFirst();
    int id = index.sccIdOf(loop.getFirst());
    for (WITUpNode n : loop) {
      assertEquals(id, index.sccIdOf(n), "component members must share an id");
    }
  }

  @Test
  public void componentsAreInTopologicalOrder() {
    WITUpGraph cpg = graphOf("<br.unb.cic.witup.samples.Array: int getChecked(int[],int)>");
    CfgSccIndex index = cpg.sccIndex();

    for (WITUpEdge edge : cfgEdgesOf(cpg)) {
      int from = index.sccIdOf(edge.getSource());
      int to = index.sccIdOf(edge.getTarget());
      if (from != to) {
        assertTrue(
            from < to,
            "inter-component edge must run forward in the order, got " + from + " -> " + to);
      }
    }
  }
}
