package br.unb.cic.witup.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.core.jimple.basic.Immediate;

/**
 * A node representing a throw statement.
 */
public class ThrowStatementNode extends WITUpNode {

    private final Immediate throwExpr;

    /**
     * Constructor for ThrowStatementNode.
     *
     * @param node the property graph node
     * @param throwExpr the throw expression
     */
    public ThrowStatementNode(final PropertyGraphNode node, final Immediate throwExpr) {
        super(node);
        this.throwExpr = throwExpr;
    }

    /**
     * Gets the throw expression.
     *
     * @return the throw expression
     */
    public Immediate getThrowExpr() {
        return throwExpr;
    }

}

