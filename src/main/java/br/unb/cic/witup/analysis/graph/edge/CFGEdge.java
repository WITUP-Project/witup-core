package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/** Control flow graph edge. */
public class CFGEdge extends WITUpEdge {

  /**
   * Constructor for CFGEdge.
   *
   * @param edge the property graph edge
   */
  public CFGEdge(final PropertyGraphEdge edge, final WITUpNode source, final WITUpNode target) {
    super(edge, source, target);
  }
}
