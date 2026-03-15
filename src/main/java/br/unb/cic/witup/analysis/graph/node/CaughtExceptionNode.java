package br.unb.cic.witup.analysis.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;

public final class CaughtExceptionNode extends WITUpNode {
  private final JCaughtExceptionRef caughtExceptionRef;

  public CaughtExceptionNode(
      final PropertyGraphNode node, final JCaughtExceptionRef caughtExceptionRef) {
    super(node);
    this.caughtExceptionRef = caughtExceptionRef;
  }

  public JCaughtExceptionRef getCaughtExceptionRef() {
    return caughtExceptionRef;
  }
}
