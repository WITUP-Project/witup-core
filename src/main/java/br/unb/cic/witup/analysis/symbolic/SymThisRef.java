package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JThisRef;

public final class SymThisRef extends SymExpr {
  private final String type;

  public SymThisRef(final JThisRef r) {
    super(SymKind.OTHER);
    type = r.getType().toString();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitThisRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return null;
  }

  @Override
  public String toString() {
    return "@this:" + type;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  public String getType() {
    return type;
  }
}
