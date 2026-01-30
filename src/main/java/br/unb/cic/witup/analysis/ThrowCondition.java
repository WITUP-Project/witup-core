package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.node.WITUpNode;

public final class ThrowCondition {
  private WITUpNode node;
  private boolean truthValue;

  public ThrowCondition(final WITUpNode node, final boolean truthValue) {
    this.node = node;
    this.truthValue = truthValue;
  }

  public WITUpNode getNode() {
    return node;
  }

  public boolean getTruthValue() {
    return truthValue;
  }
}
