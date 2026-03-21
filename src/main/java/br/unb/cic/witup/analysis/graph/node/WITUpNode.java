package br.unb.cic.witup.analysis.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;

/** Abstract base class for WITUpGraph nodes. */
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
  public final boolean equals(final Object o) {
    return this == o;
  }

  @Override
  public final int hashCode() {
    return System.identityHashCode(this);
  }

  public final PropertyGraphNode getNode() {
    return node;
  }
}
