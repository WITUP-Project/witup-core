package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.List;
import java.util.Set;

/**
 * A natural loop in the control flow graph.
 *
 * @param header the loop header node (target of the back-edge, typically the loop condition)
 * @param body all nodes belonging to the loop, including the header
 * @param backEdge the CFG edge from the loop latch back to the header
 * @param exitEdges CFG edges that leave the loop body
 */
public record NaturalLoop(
    WITUpNode header,
    Set<WITUpNode> body,
    WITUpEdge backEdge,
    List<WITUpEdge> exitEdges) {}
