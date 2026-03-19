package br.unb.cic.witup.analysis.symbolic;

import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.types.ArrayType;

/**
 * Represents an allocated array
 */
public final class SymArray extends SymExpr {
  private final String name;
  private final String objectType;

  public SymArray(final Local l) {
    super(fromJimpleType(((ArrayType) l.getType()).getElementType()));
    this.name = l.toString();
    this.objectType = l.getType().toString();
  }

  public SymArray(final JNewArrayExpr newArrExpr) {
    super(fromJimpleType(newArrExpr.getType()));
    this.name = newArrExpr.toString();
    this.objectType = newArrExpr.getType().toString();
  }

  public String getName() {
    return name;
  }

  public String getObjectType() {
    return objectType;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitArray(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean contains(final String varName) {
    return name.equals(varName);
  }
}
