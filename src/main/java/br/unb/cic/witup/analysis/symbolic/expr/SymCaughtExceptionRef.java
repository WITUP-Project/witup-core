package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;

public final class SymCaughtExceptionRef extends SymExpr {
  private final String caughtType;
  private String cachedToString;

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
    if (cachedToString == null) {
      cachedToString = "@caughtexception:" + caughtType;
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymCaughtExceptionRef symCaughtExceptionRef)) {
      return false;
    }
    return caughtType.equals(symCaughtExceptionRef.caughtType);
  }

  @Override
  public int hashCode() {
    return caughtType.hashCode();
  }
}
