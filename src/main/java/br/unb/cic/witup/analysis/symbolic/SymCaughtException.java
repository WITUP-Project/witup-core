package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;

public class SymCaughtException extends SymExpr {
  private final String caughtType;

  public SymCaughtException(JCaughtExceptionRef r) {
    super(SymKind.BOOLEAN);
    this.caughtType = r.getType().toString();
  }

  public String getCaughtType() {
    return caughtType;
  }

  @Override
  public <T> T accept(SymExprVisitor<T> visitor) {
    return visitor.visitCaughtException(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return "@caughtexception:" + caughtType;
  }

  @Override
  public boolean contains(String varName) {
    return false;
  }
}
