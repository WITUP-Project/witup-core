package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.List;

public record WITUpPath(List<WITUpNode> nodes, List<WITUpEdge> edges) {}
