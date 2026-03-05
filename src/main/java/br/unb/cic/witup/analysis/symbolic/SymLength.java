package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymLength extends SymExpr {
  private final SymExpr op;

  public SymLength(final SymExpr op) {
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
    return op.toString() + ".length";
  }

  @Override
  public boolean contains(final String varName) {
    return op.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
