package br.unb.cic.witup.analysis.symbolic;

import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.types.Type;

public final class SymParamRef extends SymExpr {
  private final int index;
  private final String paramType;
  private String cachedToString;

  public SymParamRef(final JParameterRef r) {
    super(fromJimpleType(r.getType()));
    this.index = r.getIndex();
    this.paramType = r.getType().toString();
  }

  public SymParamRef(final int index, final Type type) {
    super(fromJimpleType(type));
    this.index = index;
    this.paramType = type.toString();
  }

  public int getIndex() {
    return index;
  }

  public String getParamType() {
    return paramType;
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
    if  (cachedToString == null) {
      cachedToString = "@parameter" + this.index + ": " + this.paramType;
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return Integer.toString(index).contains(varName);
  }
}
