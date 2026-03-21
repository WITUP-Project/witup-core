package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;

/** Control dependency edge. */
public class ControlDependencyEdge extends WITUpEdge {
  public ControlDependencyEdge(final WITUpNode source, final WITUpNode target) {
    super(source, target);
  }
}
