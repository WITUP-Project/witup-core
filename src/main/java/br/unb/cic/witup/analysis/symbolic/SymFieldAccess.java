package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymFieldAccess extends SymExpr {
  private final SymExpr base; // e.g., "this" or another object
  private final String fieldName; // e.g., "radius"
  private final SymKind kind;

  public SymFieldAccess(final SymExpr base, final String fieldName, final SymKind kind) {
    this.base = base;
    this.fieldName = fieldName;
    this.kind = kind;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitField(this);
  }

  public SymExpr getBase() {
    return base;
  }

  public String getFieldName() {
    return fieldName;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    // Substitute in the base expression
    SymExpr newBase = base.substitute(varName, replacement);
    if (newBase != base) {
      return new SymFieldAccess(newBase, fieldName, kind);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName);
  }

  @Override
  public String toString() {
    return base.toString() + "." + fieldName;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymFieldAccess)) {
      return false;
    }
    SymFieldAccess symFieldAccess = (SymFieldAccess) o;
    return base.equals(symFieldAccess.base) && fieldName.equals(symFieldAccess.fieldName);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * base.hashCode() + fieldName.hashCode();
  }

  @Override
  public SymKind kind() {
    return kind;
  }
}
