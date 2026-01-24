package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.node.WITUpNode;

public class ThrowCondition {
    WITUpNode node;
    boolean truthValue;

    public ThrowCondition(WITUpNode node, boolean truthValue) {
        this.node = node;
        this.truthValue = truthValue;
    }
}
