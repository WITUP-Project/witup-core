package br.unb.cic.witup.analysis.symbolic;

public final class SymNewArray extends SymExpr {
  private final String baseType;
  private final int size;

  public SymNewArray(final String baseType, final int size) {
    this.baseType = baseType;
    this.size = size;
  }

  public String getBaseType() {
    return baseType;
  }

  public int getSize() {
    return size;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitNewArray(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return baseType + "[" + size + "]";
  }

  @Override
  public boolean contains(final String varName) {
    return baseType.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
