package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Path nodes and edges are stored in *reverse* CFG order (throw at index 0, entry last)
// because backDFS appends as it walks backward. The forward views and back-edge index
// set are derived once on first access and reused — callers (constraint generation,
// iteration analysis) hit them repeatedly per path.
public final class WITUpPath {
  private final List<WITUpNode> nodes;
  private final List<WITUpEdge> edges;
  private List<WITUpNode> cachedForwardNodes;
  private List<WITUpEdge> cachedForwardEdges;

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

  private static <T> List<T> reversedUnmodifiable(final List<T> source) {
    List<T> result = new ArrayList<>(source);
    Collections.reverse(result);
    return Collections.unmodifiableList(result);
  }
}
