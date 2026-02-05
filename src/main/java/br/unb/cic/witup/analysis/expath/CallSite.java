package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.graph.node.WITUpNode;

public record CallSite(String calleeSignature, WITUpNode callNode) {}
