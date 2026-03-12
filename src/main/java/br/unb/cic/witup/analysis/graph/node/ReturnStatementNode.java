package br.unb.cic.witup.analysis.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.common.stmt.JReturnStmt;

public final class ReturnStatementNode extends WITUpNode {
  private final Immediate op;

  public ReturnStatementNode(final PropertyGraphNode node, final JReturnStmt returnStmt) {
    super(node);
    this.op = returnStmt.getOp();
  }

  public Immediate getOp() {
    return op;
  }
}
