package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Detects natural loops in a {@link WITUpGraph} using dominator-based back-edge identification. */
public final class NaturalLoopDetector {

  private NaturalLoopDetector() {}

  /** Detect all natural loops in the given graph. */
  public static List<NaturalLoop> detect(final WITUpGraph graph) {
    if (graph.findEntryNode() == null) {
      return List.of();
    }

    Map<WITUpNode, Set<WITUpNode>> dominators = graph.computeDominators();
    List<WITUpEdge> backEdges = graph.findBackEdges(dominators);

    List<NaturalLoop> loops = new ArrayList<>();
    for (WITUpEdge backEdge : backEdges) {
      WITUpNode header = backEdge.getTarget();
      Set<WITUpNode> body = graph.computeLoopBody(header, backEdge.getSource());
      List<WITUpEdge> exitEdges = graph.computeExitEdges(body);
      loops.add(new NaturalLoop(header, body, backEdge, exitEdges));
    }
    return loops;
  }
}
