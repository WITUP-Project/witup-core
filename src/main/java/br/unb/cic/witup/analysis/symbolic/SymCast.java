package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JCastExpr;

public final class SymCast extends SymExpr {
  private final SymExpr op;
  private final String type;

  public SymCast(final JCastExpr c) {
    super(SymKind.CAST);
    op = fromJimple(c.getOp());
    type = c.getType().toString();
  }
  
  public SymExpr getOp() {
    return op;
  }

  public String getType() {
    return type;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitCast(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return "(" + type + ")" + op.toString();
  }

  @Override
  public boolean contains(final String varName) {
    return op.contains(varName);
  }
}
