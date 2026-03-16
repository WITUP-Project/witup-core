package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;

public final class SymDynamicInvoke extends SymExpr {
  private final String signature;

  public SymDynamicInvoke(final JDynamicInvokeExpr e) {
    super(SymKind.OTHER);
    this.signature = e.toString();
  }

  public String getSignature() {
    return signature;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitDynamicInvoke(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return signature;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }
}
