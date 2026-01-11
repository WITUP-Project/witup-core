package br.unb.cic.witup.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;

/**
 * A simple node implementation for basic graph nodes.
 */
public class SimpleNode extends WITUpNode {
    private final PropertyGraphNode node;
    /**
     * Constructor for SimpleNode.
     *
     * @param node the property graph node
     */
    public SimpleNode(final PropertyGraphNode node) {
        super(node);
        this.node = node;
    }

    public PropertyGraphNode getNode() {
        return this.node;
    }

}

