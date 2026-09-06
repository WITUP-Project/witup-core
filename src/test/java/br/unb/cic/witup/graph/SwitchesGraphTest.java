package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.WITUpPath;
import br.unb.cic.witup.analysis.graph.edge.CFGEdge;
import br.unb.cic.witup.analysis.graph.edge.SwitchCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SwitchesGraphTest {

  private static final String CLASSIFY =
      "<br.unb.cic.witup.samples.Switches: int classify(java.lang.String,int)>";

  @Test
  public void switchEdgesAreControlFlowEdges() {
    WITUpGraph cpg = TestAnalysisContext.getAnalyser().analyseMethod(CLASSIFY).graph();

    List<WITUpEdge> switchEdges =
        cpg.edgeSet().stream().filter(e -> e instanceof SwitchCFGEdge).toList();
    assertFalse(switchEdges.isEmpty(), "sample must compile to a switch for this test to bite");

    for (WITUpEdge edge : switchEdges) {
      assertTrue(
          edge instanceof CFGEdge,
          "a switch edge is control flow; excluding it hides everything past the switch");
    }
  }

  @Test
  public void throwsInsideSwitchArmsAreReachableFromEntry() {
    WITUpGraph cpg = TestAnalysisContext.getAnalyser().analyseMethod(CLASSIFY).graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertTrue(throwNodes.size() >= 3, "guard plus two switch arms all throw: " + throwNodes);

    for (WITUpNode throwNode : throwNodes) {
      List<WITUpPath> paths = cpg.getThrowPaths(throwNode);
      assertFalse(
          paths.isEmpty(),
          "no path reaches "
              + ((ThrowStatementNode) throwNode).getStmtPos()
              + "; a throw the analysis cannot reach is a silently dropped exception");
    }
  }
}
