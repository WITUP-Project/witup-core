package br.unb.cic.witup.analysis.symbolic;

import sootup.core.jimple.common.constant.LongConstant;

public final class SymLongConstant extends SymExpr {
  private final long value;

  public SymLongConstant(final LongConstant c) {
    super(fromJimpleType(c.getType()));
    value = c.getValue();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitLongConst(this);
  }

  public long getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }

  @Override
  public boolean contains(final String varName) {
    return Long.toString(value).contains(varName);
  }
}
