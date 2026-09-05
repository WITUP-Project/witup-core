package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.types.Type;

public final class SymParamRef extends SymExpr {
  /**
   * -1 is recerved for the receiver of the method. Matches buildFormals and how we pass actuals
   * in. We will probably have issues with >= 64 parameters. Fix if there is empirical need
   */
  public static final int THIS_INDEX = -1;

  private final int index;
  private final String paramType;
  private final String name;
  private String cachedToString;

  public SymParamRef(final JParameterRef r) {
    super(fromJimpleType(r.getType()), 1L << r.getIndex());
    this.index = r.getIndex();
    this.paramType = r.getType().toString();
    this.name = null;
  }

  public SymParamRef(final int index, final Type type) {
    super(fromJimpleType(type), 1L << index);
    this.index = index;
    this.paramType = type.toString();
    this.name = null;
  }

  private SymParamRef(
          final int index, final String paramType, final SymKind kind, final String name) {
    super(kind, 1L << index);
    this.index = index;
    this.paramType = paramType;
    this.name = name;
  }

  public SymParamRef withName(final String sourceName) {
    return new SymParamRef(index, paramType, getKind(), sourceName);
  }

  public int getIndex() {
    return index;
  }

  public String getParamType() {
    return paramType;
  }

  public String getName() {
    return name;
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
    if (cachedToString == null) {
      cachedToString = name != null ? name : "@parameter" + this.index + ": " + this.paramType;
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    // A resolved parameter is not a free local. The `name` field display only and deliberately
    // does not participate. Otherwise we would have issues with backward substitution
    return false;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymParamRef symParamRef)) {
      return false;
    }
    return index == symParamRef.index && paramType.equals(symParamRef.paramType);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * Integer.hashCode(index) + paramType.hashCode();
  }
}
