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

// Path nodes and edges are stored in *reverse* CFG order (throw at index 0, entry last)
// because backDFS appends as it walks backward. The forward views and back-edge index
// set are derived once on first access and reused — callers (constraint generation,
// iteration analysis) hit them repeatedly per path.
public final class WITUpPath {
  private final List<WITUpNode> nodes;
  private final List<WITUpEdge> edges;
  private List<WITUpNode> cachedForwardNodes;
  private List<WITUpEdge> cachedForwardEdges;
  private Set<Integer> cachedBackEdgeForwardIndices;

  public WITUpPath(final List<WITUpNode> nodes, final List<WITUpEdge> edges) {
    this.nodes = nodes;
    this.edges = edges;
  }

  public List<WITUpNode> nodes() {
    return nodes;
  }

  public List<WITUpEdge> edges() {
    return edges;
  }

  public List<WITUpNode> forwardNodes() {
    List<WITUpNode> cached = cachedForwardNodes;
    if (cached == null) {
      cached = reversedUnmodifiable(nodes);
      cachedForwardNodes = cached;
    }
    return cached;
  }

  public List<WITUpEdge> forwardEdges() {
    List<WITUpEdge> cached = cachedForwardEdges;
    if (cached == null) {
      cached = reversedUnmodifiable(edges);
      cachedForwardEdges = cached;
    }
    return cached;
  }

  // Forward indices of edges that "go backward" in the path — i.e. their target was
  // already seen before the edge's position. These mark loop back-edge crossings on
  // the enumerated path; one entry per back-edge traversal.
  public Set<Integer> backEdgeForwardIndices() {
    Set<Integer> cached = cachedBackEdgeForwardIndices;
    if (cached == null) {
      cached = computeBackEdgeForwardIndices();
      cachedBackEdgeForwardIndices = cached;
    }
    return cached;
  }

  private Set<Integer> computeBackEdgeForwardIndices() {
    List<WITUpNode> forwardNodesList = forwardNodes();
    List<WITUpEdge> forwardEdgesList = forwardEdges();
    Map<WITUpNode, Integer> firstSeenAt = new HashMap<>();
    for (int i = 0; i < forwardNodesList.size(); i++) {
      firstSeenAt.putIfAbsent(forwardNodesList.get(i), i);
    }
    Set<Integer> result = new TreeSet<>();
    for (int i = 0; i < forwardEdgesList.size(); i++) {
      WITUpEdge e = forwardEdgesList.get(i);
      Integer firstTarget = firstSeenAt.get(e.getTarget());
      // The "natural" forward edge into position i+1 has its target appear at i+1 for
      // the first time. If the target was already seen earlier, edge[i] is rewinding
      // the path to a previously-visited node — i.e. a back-edge crossing.
      if (firstTarget != null && firstTarget < i + 1) {
        result.add(i);
      }
    }
    return Collections.unmodifiableSet(result);
  }

  private static <T> List<T> reversedUnmodifiable(final List<T> source) {
    List<T> result = new ArrayList<>(source);
    Collections.reverse(result);
    return Collections.unmodifiableList(result);
  }
}
