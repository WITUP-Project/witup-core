package br.unb.cic.witup.analysis;

public final class SymVirtualInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String invokeName; // e.g. length
  private final boolean returnsBoolean;

  public SymExpr getBase() { return base; }

  public String getInvokeName() { return invokeName; }

  public SymVirtualInvoke(SymExpr base, String invokeName, boolean returnsBoolean) {
    this.base = base;
    this.invokeName = invokeName;
    this.returnsBoolean = returnsBoolean;
  }

  @Override
  public SymExpr substitute(final String invokeName, final SymExpr replacement) {
    SymExpr newBase = base.substitute(invokeName, replacement);
    if (newBase != base) {
      return new SymVirtualInvoke(newBase, invokeName, this.returnsBoolean);
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

  @Override
  public SymKind kind() {
    return returnsBoolean ? SymKind.BOOLEAN : SymKind.OTHER;
  }
}
