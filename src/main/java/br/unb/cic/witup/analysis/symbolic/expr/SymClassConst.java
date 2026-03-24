package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.constant.ClassConstant;

public final class SymClassConst extends SymExpr {
  private final String value;
  private final String type;

  public SymClassConst(final ClassConstant c) {
    super(SymKind.OTHER);
    this.value = c.getValue();
    this.type = c.getType().toString();
  }

  public String getValue() {
    return value;
  }

  public String getType() {
    return type;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitClassConst(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }
}
