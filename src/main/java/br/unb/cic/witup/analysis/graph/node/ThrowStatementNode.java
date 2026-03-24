package br.unb.cic.witup.analysis.graph.node;

import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.common.stmt.JThrowStmt;

/** A node representing a throw statement. */
public final class ThrowStatementNode extends WITUpNode {
  private final Immediate local;
  private final StmtPositionInfo stmtPos;

  public ThrowStatementNode(final PropertyGraphNode node, final JThrowStmt throwStmt) {
    super(node);
    // this will generate something like #l1 = (java.lang.Throwable) $stack5,
    // so we can trace back to the type when we have the WITUpGraph
    this.local = throwStmt.getOp();
    this.stmtPos = throwStmt.getPositionInfo();
  }

  public Immediate getLocal() {
    return local;
  }

  public StmtPositionInfo getStmtPos() {
    return stmtPos;
  }
}
