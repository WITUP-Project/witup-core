package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;

public class GotoCFGEdge extends CFGEdge {
  public GotoCFGEdge(final WITUpNode source, final WITUpNode target) {
    super(source, target);
  }
}
