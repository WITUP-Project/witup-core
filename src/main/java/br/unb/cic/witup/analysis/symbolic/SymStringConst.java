package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymStringConst extends SymExpr {
  private final String value;

  public SymStringConst(final String value) {
    super(SymKind.STRING);
    this.value = value;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitStringConst(this);
  }

  public String getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public String toString() {
    return "'" + value + "'";
  }
}
