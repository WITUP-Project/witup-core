package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;

public final class SymCaughtExceptionRef extends SymExpr {
  private final String caughtType;

  public SymCaughtExceptionRef(final JCaughtExceptionRef r) {
    super(SymKind.BOOLEAN);
    this.caughtType = r.getType().toString();
  }

  public String getCaughtType() {
    return caughtType;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitCaughtException(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return "@caughtexception:" + caughtType;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }
}
