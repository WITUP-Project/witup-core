package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JThisRef;

public final class SymThisRef extends SymExpr {
  private final String type;
  private final String cachedToString;

  public SymThisRef(final JThisRef r) {
    super(SymKind.OTHER);
    type = r.getType().toString();
    this.cachedToString = "@this:" + type;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitThisRef(this);
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

  public String getType() {
    return type;
  }
}
