package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JCastExpr;

public final class SymCast extends SymExpr {
  private final SymExpr op;
  private final String type;
  private String cachedToString;

  public SymCast(final JCastExpr c) {
    super(SymKind.CAST);
    op = fromJimple(c.getOp());
    type = c.getType().toString();
  }

  private SymCast(final SymExpr op, final String type) {
    super(SymKind.CAST);
    this.op = op;
    this.type = type;
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
    SymExpr newOp = op.substitute(varName, replacement);
    if (newOp != op) {
      return new SymCast(newOp, type);
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newOp = op.substituteParam(idx, actual);
    if (newOp != op) {
      return new SymCast(newOp, type);
    }
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = "(" + type + ")" + op.toString();
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return op.contains(varName);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymCast sc)) {
      return false;
    }
    return type.equals(sc.type) && op.equals(sc.op);
  }

  @Override
  public int hashCode() {
    return HASH_PRIME * type.hashCode() + op.hashCode();
  }
}
