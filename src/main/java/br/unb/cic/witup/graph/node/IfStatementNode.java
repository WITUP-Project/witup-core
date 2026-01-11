package br.unb.cic.witup.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.core.jimple.common.expr.AbstractConditionExpr;

/**
 * A node representing an if statement.
 */
public class IfStatementNode extends WITUpNode {
    private final AbstractConditionExpr condition;
    private final PropertyGraphNode node;

    /**
     * Constructor for IfStatementNode.
     *
     * @param node a property graph node
     * @param condition the condition expression evaluated by the node
     */
    public IfStatementNode(final PropertyGraphNode node, final AbstractConditionExpr condition) {
        super(node);
        this.condition = condition;
        this.node = node;
    }

    /**
     * Gets the condition expression.
     *
     * @return the condition expression
     */
    public AbstractConditionExpr getCondition() {
        return condition;
    }

    public PropertyGraphNode getNode() {
        return this.node;
    }
}
