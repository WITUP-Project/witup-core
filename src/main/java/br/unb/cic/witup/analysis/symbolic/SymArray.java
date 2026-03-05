package br.unb.cic.witup.analysis.symbolic;

public final class SymArray extends SymExpr {
  private final String name;
  private final SymKind elemKind;
  private final String objectType;

  public SymArray(String name, SymKind elemKind,  String objectType) {
    this.name = name;
    this.elemKind = elemKind;
    this.objectType = objectType;
  }

  public String getName() {
    return name;
  }

  public SymKind getElementKind() {
    return elemKind;
  }

  public String getObjectType() { return objectType; }

  @Override
  public <T> T accept(SymExprVisitor<T> visitor) {
    return visitor.visitArray(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean contains(String varName) {
    return name.equals(varName);
  }

  @Override
  public SymKind kind() {
    return elemKind;
  }
}
