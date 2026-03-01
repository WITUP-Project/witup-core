package br.unb.cic.witup.analysis.symbolic;

public final class SymArrayRef extends SymExpr {
  private final SymExpr base;
  private final String index;

  public SymArrayRef(final SymExpr base, final String index) {
    this.base = base;
    this.index = index;
  }

  public SymExpr getBase() {
    return base;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitArrayRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newBase = base.substitute(varName, replacement);
    if (newBase != base) {
      return new SymArrayRef(newBase, index);
    }
    return this;
  }

  @Override
  public String toString() {
    return base.toString() + "[" + index + "]";
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
