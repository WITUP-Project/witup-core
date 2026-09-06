package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.core.jimple.basic.Local;

// weak modelling of dereferences
public record ImplicitNpeReceiverSite(WITUpNode node, Local receiver) {}
