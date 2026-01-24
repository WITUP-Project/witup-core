package br.unb.cic.witup.graph.edge;

import br.unb.cic.witup.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/**
 * Control dependency edge.
 */
public class ControlDependencyEdge extends WITUpEdge {

    /**
     * Constructor for ControlDependencyEdge.
     *
     * @param edge the property graph edge
     */
    public ControlDependencyEdge(final PropertyGraphEdge edge, WITUpNode source, WITUpNode target) {
        super(edge,  source, target);
    }

}
