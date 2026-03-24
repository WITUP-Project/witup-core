package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JLengthExpr;

public final class SymLength extends SymExpr {
  private final SymExpr op;
  private String cachedToString;

  public SymLength(final JLengthExpr e) {
    super(SymKind.INT);
    this.op = fromJimple(e.getOp());
  }

  private SymLength(final SymExpr op) {
    super(SymKind.INT);
    this.op = op;
  }

  public SymExpr getOp() {
    return op;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitLength(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newOp = op.substitute(varName, replacement);
    if (newOp != op) {
      return new SymLength(newOp);
    }
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = op.toString() + ".length";
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return op.contains(varName);
  }
}
