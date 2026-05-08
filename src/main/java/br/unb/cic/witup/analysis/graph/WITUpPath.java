package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public record WITUpPath(List<WITUpNode> nodes, List<WITUpEdge> edges) {

  // Path nodes are stored in *reverse* CFG order (throw at index 0, entry last) because
  // backDFS builds the path while walking backward. Same goes for edges. These helpers
  // expose the forward view that's natural for talking about loop iterations.

  public List<WITUpNode> forwardNodes() {
    List<WITUpNode> reversed = new ArrayList<>(nodes);
    Collections.reverse(reversed);
    return reversed;
  }

  public List<WITUpEdge> forwardEdges() {
    List<WITUpEdge> reversed = new ArrayList<>(edges);
    Collections.reverse(reversed);
    return reversed;
  }

  // Forward indices of edges that "go backward" in the path — i.e. their target was
  // already seen before the edge's position. These mark loop back-edge crossings on
  // the enumerated path; one entry per back-edge traversal.
  public Set<Integer> backEdgeForwardIndices() {
    List<WITUpNode> forwardNodes = forwardNodes();
    List<WITUpEdge> forwardEdges = forwardEdges();
    Map<WITUpNode, Integer> firstSeenAt = new HashMap<>();
    for (int i = 0; i < forwardNodes.size(); i++) {
      firstSeenAt.putIfAbsent(forwardNodes.get(i), i);
    }
    Set<Integer> result = new TreeSet<>();
    for (int i = 0; i < forwardEdges.size(); i++) {
      WITUpEdge e = forwardEdges.get(i);
      Integer firstTarget = firstSeenAt.get(e.getTarget());
      if (firstTarget != null && firstTarget < i + 1) {
        // The "natural" forward edge into position i+1 has its target appear at i+1 for
        // the first time. If the target was already seen earlier, edge[i] is rewinding
        // the path to a previously-visited node — i.e. a back-edge crossing.
        result.add(i);
      }
    }
    return result;
  }
}
