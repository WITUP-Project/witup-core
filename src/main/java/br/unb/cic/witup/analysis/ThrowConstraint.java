package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.node.WITUpNode;

public record ThrowConstraint(WITUpNode node, boolean truthValue) {
}
