package br.unb.cic.witup.analysis.expath.records;

import br.unb.cic.witup.graph.node.WITUpNode;
import java.util.List;

public record CallSite(String calleeSignature, List<String> args, WITUpNode callNode) {}
