package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymParam extends SymExpr {
  private final int index;
  private final SymKind kind;

  public SymParam(final int index, final SymKind kind) {
    this.index = index;
    this.kind = kind;
  }

  public int getIndex() {
    return index;
  }

  public SymKind getKind() {
    return kind;
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

  @Override
  public SymKind kind() {
    return kind;
  }
}
