package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JNewExpr;

public final class SymNew extends SymExpr {
  private final String cachedToString;

  public SymNew(final JNewExpr expr) {
    super(SymKind.OTHER);
    String classType = expr.getType().toString();
    this.cachedToString = "new " + classType;
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
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  // Equality keys on cachedToString because it carries the only piece of state distinguishing
  // two SymNew instances (the formatted class type, e.g. "new java.lang.IllegalArgumentException").
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymNew symNew)) {
      return false;
    }
    return cachedToString.equals(symNew.cachedToString);
  }

  @Override
  public int hashCode() {
    return cachedToString.hashCode();
  }
}
