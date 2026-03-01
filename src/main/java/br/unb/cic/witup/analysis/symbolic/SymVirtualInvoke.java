package br.unb.cic.witup.analysis.symbolic;

public final class SymVirtualInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String invokeName; // e.g. length
  private final boolean returnsBoolean;

  public SymExpr getBase() {
    return base;
  }

  public String getInvokeName() {
    return invokeName;
  }

  public SymVirtualInvoke(
      final SymExpr base, final String invokeName, final boolean returnsBoolean) {
    this.base = base;
    this.invokeName = invokeName;
    this.returnsBoolean = returnsBoolean;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitVirtualInvoke(this);
  }

  @Override
  public SymExpr substitute(final String invField, final SymExpr replacement) {
    SymExpr newBase = base.substitute(invField, replacement);
    if (newBase != base) {
      return new SymVirtualInvoke(newBase, invField, this.returnsBoolean);
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
    return returnsBoolean ? SymKind.BOOLEAN_METHOD : SymKind.OTHER;
  }
}
