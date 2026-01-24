package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;

public class ThrowCondition {
    PropertyGraphNode node;
    boolean truthValue;

    public ThrowCondition(PropertyGraphNode node, boolean truthValue) {
        this.node = node;
        this.truthValue = truthValue;
    }
}
