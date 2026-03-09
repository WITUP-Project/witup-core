package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/** Data dependency edge. */
public class DataDependencyEdge extends WITUpEdge {

  /**
   * Constructor for DataDependencyEdge.
   *
   * @param edge the property graph edge
   */
  public DataDependencyEdge(
      final PropertyGraphEdge edge, final WITUpNode source, final WITUpNode target) {
    super(edge, source, target);
  }
}
