package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JParameterRef;

public final class SymParamRef extends SymExpr {
  private final int index;

  public SymParamRef(final JParameterRef r) {
    super(symKindFromType(r.getType()));
    this.index = r.getIndex();
  }

  public SymParamRef(final int index, final SymKind kind) {
    super(kind);
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitParamRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    return this.index == idx ? actual : this;
  }

  @Override
  public String toString() {
    return "@parameter" + index;
  }

  @Override
  public boolean contains(final String varName) {
    return Integer.toString(index).contains(varName);
  }
}
