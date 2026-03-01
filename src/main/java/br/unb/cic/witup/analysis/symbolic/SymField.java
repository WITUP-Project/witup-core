package br.unb.cic.witup.analysis.symbolic;

public final class SymField extends SymExpr {
  private final SymExpr base; // e.g., "this" or another object
  private final String fieldName; // e.g., "radius"

  public SymField(final SymExpr base, final String fieldName) {
    this.base = base;
    this.fieldName = fieldName;
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
      return new SymField(newBase, fieldName);
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
    if (!(o instanceof SymField)) {
      return false;
    }
    SymField symField = (SymField) o;
    return base.equals(symField.base) && fieldName.equals(symField.fieldName);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * base.hashCode() + fieldName.hashCode();
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
