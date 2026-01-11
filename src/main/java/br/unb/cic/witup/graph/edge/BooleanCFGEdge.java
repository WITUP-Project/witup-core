package br.unb.cic.witup.graph.edge;

import java.util.Objects;

import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/**
 * A control flow graph edge with a boolean condition.
 */
public class BooleanCFGEdge extends CFGEdge {
    private final boolean condition;

    /**
     * Constructor for BooleanCFGEdge.
     *
     * @param edge the property graph edge
     * @param condition the boolean condition
     */
    public BooleanCFGEdge(final PropertyGraphEdge edge, final boolean condition) {
        super(edge);
        this.condition = condition;
    }

    /**
     * Gets the boolean condition.
     *
     * @return the condition
     */
    public boolean getCondition() {
        return condition;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BooleanCFGEdge edge = (BooleanCFGEdge) o;
        return super.equals(edge) && condition == edge.condition;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), condition);
    }

}
