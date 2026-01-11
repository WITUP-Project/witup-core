package br.unb.cic.witup.graph.edge;

import java.util.Objects;

import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/**
 * Abstract base class for WITUpGraph edges. Wraps SootUp PropertyGraphEdge type
 */
public abstract class WITUpEdge {
    private final PropertyGraphEdge edge;

    /**
     * Constructor for WITUpEdge.
     *
     * @param edge the property graph edge
     */
    public WITUpEdge(final PropertyGraphEdge edge) {
        this.edge = edge;
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

