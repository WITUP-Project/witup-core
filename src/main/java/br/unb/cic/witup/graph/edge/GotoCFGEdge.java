package br.unb.cic.witup.graph.edge;

import br.unb.cic.witup.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

public class GotoCFGEdge extends CFGEdge {
  public GotoCFGEdge(final PropertyGraphEdge edge,
                     final WITUpNode source,
                     final WITUpNode target) {
    super(edge, source, target);
  }
}
