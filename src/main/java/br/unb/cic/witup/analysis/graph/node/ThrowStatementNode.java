package br.unb.cic.witup.analysis.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.core.jimple.basic.Immediate;

/** A node representing a throw statement. */
public class ThrowStatementNode extends WITUpNode {

  private final Immediate local;

  /**
   * Constructor for ThrowStatementNode.
   *
   * @param node the property graph node
   * @param local the throw expression
   */
  public ThrowStatementNode(final PropertyGraphNode node, final Immediate local) {
    super(node);
    // this will generate something like #l1 = (java.lang.Throwable) $stack5,
    // so we can trace back to the type
    this.local = local;
  }

  /**
   * Gets the throw expression.
   *
   * @return the throw expression
   */
  public Immediate getLocal() {
    return local;
  }
}
