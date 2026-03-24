package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymNull extends SymExpr {
  public static final SymNull INSTANCE = new SymNull();
  private String cachedToString = "null";

  private SymNull() {
    super(SymKind.NULL);
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitNull(this);
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
    return cachedToString;
  }
}
