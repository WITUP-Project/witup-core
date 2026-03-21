package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;

/** Control flow graph edge. */
public class CFGEdge extends WITUpEdge {
  public CFGEdge(final WITUpNode source, final WITUpNode target) {
    super(source, target);
  }
}
