package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymArray extends SymExpr {
  private final String name;
  private final SymKind elemKind;
  private final String objectType;

  public SymArray(final String name, final SymKind elemKind, final String objectType) {
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

  public String getObjectType() {
    return objectType;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitArray(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean contains(final String varName) {
    return name.equals(varName);
  }

  @Override
  public SymKind kind() {
    return elemKind;
  }
}
