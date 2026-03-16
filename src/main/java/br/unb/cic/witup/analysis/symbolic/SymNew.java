package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JNewExpr;

public final class SymNew extends SymExpr {
  private final String classType;

  public SymNew(final JNewExpr expr) {
    super(SymKind.OTHER);
    classType = expr.getType().toString();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitNewRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return "new " + classType;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }
}
