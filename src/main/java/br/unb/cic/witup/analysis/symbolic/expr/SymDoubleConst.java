package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import sootup.core.jimple.common.constant.DoubleConstant;

public final class SymDoubleConst extends SymExpr {
  private final double value;
  private String cachedToString;

  public SymDoubleConst(final DoubleConstant c) {
    super(fromJimpleType(c.getType()));
    value = c.getValue();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitDoubleConst(this);
  }

  public double getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = Double.toString(value);
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return Double.toString(value).contains(varName);
  }
}
