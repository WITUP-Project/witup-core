package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/** Control dependency edge. */
public class ControlDependencyEdge extends WITUpEdge {

  /**
   * Constructor for ControlDependencyEdge.
   *
   * @param edge the property graph edge
   */
  public ControlDependencyEdge(
      final PropertyGraphEdge edge, final WITUpNode source, final WITUpNode target) {
    super(edge, source, target);
  }
}
