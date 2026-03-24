package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import sootup.core.jimple.common.constant.FloatConstant;

public final class SymFloatConst extends SymExpr {
  private final float value;
  private String cachedToString;

  public SymFloatConst(final FloatConstant c) {
    super(fromJimpleType(c.getType()));
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
    if (cachedToString == null) {
      cachedToString = Float.toString(value);
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return Float.toString(value).contains(varName);
  }
}
