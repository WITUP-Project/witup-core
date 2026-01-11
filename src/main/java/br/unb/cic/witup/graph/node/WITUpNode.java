package br.unb.cic.witup.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;

import java.util.Objects;

/**
 * Abstract base class for WITUpGraph nodes.
 */
public abstract class WITUpNode {
    private final PropertyGraphNode node;

    /**
     * Constructor for WITUpNode.
     *
     * @param node a SootUp PropertyGraphNode
     */
    public WITUpNode(final PropertyGraphNode node) {
        this.node = node;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WITUpNode node1 = (WITUpNode) o;
        return Objects.equals(node, node1.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node);
    }

    public PropertyGraphNode getNode() {
        return node;
    }
}

