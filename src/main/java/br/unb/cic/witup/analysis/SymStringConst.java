package br.unb.cic.witup.analysis;

public final class SymStringConst extends SymExpr {
  private final String value;

  public SymStringConst(final String value) {
    this.value = value;
  }

  @Override
  public <T> T accept(SymExprVisitor<T> visitor) {
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

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
