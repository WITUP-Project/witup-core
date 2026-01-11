package br.unb.cic.witup.graph.edge;

import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/**
 * Data dependency edge.
 */
public class DataDependencyEdge extends WITUpEdge {

    /**
     * Constructor for DataDependencyEdge.
     *
     * @param edge the property graph edge
     */
    public DataDependencyEdge(final PropertyGraphEdge edge) {
        super(edge);
    }

}
