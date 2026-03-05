package br.unb.cic.witup.analysis.symbolic;

public final class SymArrayRef extends SymExpr {
  private final SymExpr array;
  private final SymExpr index;

  public SymArrayRef(SymExpr array, SymExpr index) {
    this.array = array;
    this.index = index;
  }

  public SymExpr getArray() {
    return array;
  }

  public SymExpr getIndex() {
    return index;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitArrayRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return array.toString() + "[" + index + "]";
  }

  @Override
  public boolean contains(final String varName) {
    return array.toString().contains(varName);
  }

  @Override
  public SymKind kind() {
    if (array instanceof SymArray arr) {
      return arr.getElementKind();
    }
    return SymKind.OTHER;
  }
}
