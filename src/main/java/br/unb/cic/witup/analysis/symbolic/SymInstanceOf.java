package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JInstanceOfExpr;

public final class SymInstanceOf extends SymExpr {
  private final SymExpr op;
  private final String type;

  public SymInstanceOf(final JInstanceOfExpr e) {
    super(SymKind.BOOLEAN);
    this.op = fromJimple(e.getOp());
    this.type = e.getCheckType().toString();
  }

  public SymExpr getOp() {
    return op;
  }

  public String getType() {
    return type;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitInstanceOf(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return op.toString() + "_instanceof_" + type.replace(".", "_");
  }

  @Override
  public boolean contains(final String varName) {
    return op.contains(varName);
  }
}
