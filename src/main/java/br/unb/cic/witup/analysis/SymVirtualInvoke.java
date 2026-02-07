package br.unb.cic.witup.analysis;

public final class SymVirtualInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String invokeName; // e.g. length

  public SymExpr getBase() { return base; }

  public String getInvokeName() { return invokeName; }

  public SymVirtualInvoke(SymExpr base, String invokeName) {
    this.base = base;
    this.invokeName = invokeName;
  }

  @Override
  public SymExpr substitute(final String invokeName, final SymExpr replacement) {
    SymExpr newBase = base.substitute(invokeName, replacement);
    if (newBase != base) {
      return new SymVirtualInvoke(newBase, invokeName);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName);
  }

  @Override
  public String toString() {
    return base.toString() + "." + invokeName;
  }
}
