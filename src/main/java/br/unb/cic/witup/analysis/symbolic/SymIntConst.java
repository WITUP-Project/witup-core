package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.constant.IntConstant;

public final class SymIntConst extends SymExpr {
  private final int value;
  private String cachedToString;

  public static SymIntConst one() {
    return new SymIntConst(1);
  }

  public static SymIntConst zero() {
    return new SymIntConst(0);
  }

  public SymIntConst(final IntConstant c) {
    super(fromJimpleType(c.getType()));
    value = c.getValue();
  }

  private SymIntConst(final int value) {
    super(SymKind.INT);
    this.value = value;
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
    if   (cachedToString == null) {
      cachedToString = Integer.toString(value);
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return Integer.toString(value).contains(varName);
  }
}
