package br.unb.cic.witup.analysis;

public final class SymConst extends SymExpr {
  private final Object value; // Can be Integer, Double, String, etc. We need to keep track of types somewhere

  public SymConst(final Object value) {
    this.value = value;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    // Constants don't contain variables
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymConst)) {
      return false;
    }
    SymConst symConst = (SymConst) o;
    return value.equals(symConst.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
