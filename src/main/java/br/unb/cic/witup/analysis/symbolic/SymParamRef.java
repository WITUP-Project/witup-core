package br.unb.cic.witup.analysis.symbolic;

import sootup.core.jimple.common.ref.JParameterRef;

public final class SymParamRef extends SymExpr {
  private final int index;
  private final String paramType;

  public SymParamRef(final JParameterRef r) {
    super(fromJimpleType(r.getType()));
    this.index = r.getIndex();
    this.paramType = r.getType().toString();
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
    return "@parameter" + this.index + ": " + this.paramType;
  }

  @Override
  public boolean contains(final String varName) {
    return Integer.toString(index).contains(varName);
  }
}
