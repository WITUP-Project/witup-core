package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import sootup.core.jimple.common.ref.JStaticFieldRef;

public final class SymStaticFieldRef extends SymExpr {
  private final String fieldSignature;

  public SymStaticFieldRef(final JStaticFieldRef r) {
    super(fromJimpleType(r.getType()));
    this.fieldSignature = r.getFieldSignature().toString();
  }

  public String getFieldSignature() {
    return fieldSignature;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitStaticFieldRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return fieldSignature;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymStaticFieldRef symStaticFieldRef)) {
      return false;
    }
    return fieldSignature.equals(symStaticFieldRef.fieldSignature);
  }

  @Override
  public int hashCode() {
    return fieldSignature.hashCode();
  }
}
