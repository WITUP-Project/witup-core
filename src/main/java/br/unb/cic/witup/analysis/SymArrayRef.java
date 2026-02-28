package br.unb.cic.witup.analysis;

public class SymArrayRef extends SymExpr {
  SymExpr base;
  String index;

  public SymArrayRef(SymExpr base, String index) {
    this.base = base;
    this.index = index;
  }

  public SymExpr getBase() {
    return base;
  }

  @Override
  public <T> T accept(SymExprVisitor<T> visitor) {
    return visitor.visitArrayRef(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
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
  public boolean contains(String varName) {
    return base.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
