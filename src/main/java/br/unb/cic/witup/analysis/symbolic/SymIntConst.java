package br.unb.cic.witup.analysis.symbolic;

import sootup.core.jimple.common.constant.IntConstant;

public final class SymIntConst extends SymExpr {
  private final int value;

  public SymIntConst(final IntConstant c) {
    super(fromJimpleType(c.getType()));
     value = c.getValue();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitIntConst(this);
  }

  public int getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return Integer.toString(value);
  }

  @Override
  public boolean contains(final String varName) {
    return Integer.toString(value).contains(varName);
  }
}
