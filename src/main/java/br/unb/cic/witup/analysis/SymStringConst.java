package br.unb.cic.witup.analysis;

public final class SymStringConst extends SymExpr {
  private final String value;

  public SymStringConst(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    return this;
  }

  @Override
  public boolean contains(String varName) {
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
