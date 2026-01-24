package br.unb.cic.witup.graph.edge;

import java.util.Objects;

import br.unb.cic.witup.graph.node.WITUpNode;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/**
 * Abstract base class for WITUpGraph edges. Wraps SootUp PropertyGraphEdge type
 */
public abstract class WITUpEdge {
    private final PropertyGraphEdge edge;
    private final WITUpNode source;
    private final WITUpNode target;

    /**
     * Constructor for WITUpEdge.
     *
     * @param edge the property graph edge
     */
    public WITUpEdge(final PropertyGraphEdge edge, WITUpNode source, WITUpNode target) {
        this.edge = edge;
        this.source = source;
        this.target = target;
    }

    public WITUpNode getSource() {
        return source;
    }

    public WITUpNode getTarget() {
        return target;
    }

    /**
     * Gets the underlying property graph edge.
     *
     * @return the property graph edge
     */
    public PropertyGraphEdge getEdge() {
        return edge;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WITUpEdge otherEdge = (WITUpEdge) o;
        return Objects.equals(this.edge, otherEdge.edge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edge);
    }
}

