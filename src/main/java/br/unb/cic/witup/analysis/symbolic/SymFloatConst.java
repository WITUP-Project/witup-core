package br.unb.cic.witup.analysis.symbolic;

import sootup.core.jimple.common.constant.FloatConstant;

public final class SymFloatConst extends SymExpr {
  private final float value;

  public SymFloatConst(final FloatConstant c) {
    super(symKindFromType(c.getType()));
    value = c.getValue();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitFloatConst(this);
  }

  public float getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return Float.toString(value);
  }

  @Override
  public boolean contains(final String varName) {
    return Float.toString(value).contains(varName);
  }
}
