package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class PathConditionIndex {
  private final Map<WITUpNode, List<PathFact>> factsByNode;

  PathConditionIndex(final Map<WITUpNode, List<PathFact>> factsByNode) {
    this.factsByNode = factsByNode;
  }

  public List<PathFact> factsAt(final WITUpNode node) {
    return factsByNode.getOrDefault(node, List.of());
  }

  static PathConditionIndex of(final Map<WITUpNode, List<PathFact>> factsByNode) {
    return new PathConditionIndex(new IdentityHashMap<>(factsByNode));
  }
}
