package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.constant.StringConstant;

public final class SymStringConst extends SymExpr {
  private final String value;

  // we have access to c.getType() if we need

  public SymStringConst(final StringConstant c) {
    super(SymKind.STRING);
    this.value = c.getValue();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitStringConst(this);
  }

  public String getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public String toString() {
    return "'" + value + "'";
  }
}
