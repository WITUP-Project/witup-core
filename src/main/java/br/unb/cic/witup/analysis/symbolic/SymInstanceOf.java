package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymInstanceOf extends SymExpr {
  private final SymExpr op;
  private final String type;

  public SymInstanceOf(final SymExpr op, final String type) {
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

  @Override
  public SymKind kind() {
    return SymKind.BOOLEAN;
  }
}
