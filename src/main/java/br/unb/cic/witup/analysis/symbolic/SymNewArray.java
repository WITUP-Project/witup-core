package br.unb.cic.witup.analysis.symbolic;

public class SymNewArray extends SymExpr {
  String baseType;
  int size;

  public SymNewArray(String baseType, int size) {
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
  public <T> T accept(SymExprVisitor<T> visitor) {
    return visitor.visitNewArray(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return baseType + "[" + size + "]";
  }

  @Override
  public boolean contains(String varName) {
    return baseType.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
