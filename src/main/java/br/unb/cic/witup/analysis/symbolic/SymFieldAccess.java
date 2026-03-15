package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JInstanceFieldRef;

public final class SymFieldAccess extends SymExpr {
  private final SymExpr base; // e.g., "this" or another object
  private final String fieldName; // e.g., "radius"

  public SymFieldAccess(final SymExpr base, final JInstanceFieldRef r) {
    super(fromJimpleType(r.getType()));
    this.base = base;
    this.fieldName = r.getFieldSignature().getName();
  }

  private SymFieldAccess(final SymExpr value, final String fieldName, final SymKind kind) {
    super(kind);
    this.fieldName = fieldName;
    this.base = value;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitFieldAccess(this);
  }

  public SymExpr getBase() {
    return base;
  }

  public String getFieldName() {
    return fieldName;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    System.out.println("SymFieldAccess.substitute: base=" + base.toString() + "varName=" + varName.toString());
    // Substitute in the base expression
    SymExpr newBase = base.substitute(varName, replacement);
    if (newBase != base) {
      return new SymFieldAccess(newBase, fieldName, getKind());
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
    if (!(o instanceof SymFieldAccess symFieldAccess)) {
      return false;
    }
    return base.equals(symFieldAccess.base) && fieldName.equals(symFieldAccess.fieldName);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * base.hashCode() + fieldName.hashCode();
  }
}
