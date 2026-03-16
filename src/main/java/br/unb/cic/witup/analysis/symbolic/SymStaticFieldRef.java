package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JStaticFieldRef;

public class SymStaticFieldRef extends SymExpr {
  private final String fieldSignature;

  public SymStaticFieldRef(JStaticFieldRef r) {
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
}
