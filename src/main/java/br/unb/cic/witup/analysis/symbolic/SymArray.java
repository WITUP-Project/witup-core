package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JNewArrayExpr;

/** Represents an allocated array */
public final class SymArray extends SymExpr {
  private final String name;
  private final String objectType;
  private final SymExpr size;
  private String cachedToString;

  public SymArray(final JNewArrayExpr newArrExpr) {
    super(fromJimpleType(newArrExpr.getType()));
    this.name = "newarray";
    this.objectType = newArrExpr.getType().toString();
    this.size = fromJimple(newArrExpr.getSize());
  }

  private SymArray(
      final String name, final String objectType, final SymExpr size, final SymKind kind) {
    super(kind);
    this.name = name;
    this.objectType = objectType;
    this.size = size;
  }

  public String getName() {
    return name;
  }

  public String getObjectType() {
    return objectType;
  }

  public SymExpr getSize() {
    return size;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitArray(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    if (size == null) {
      return this;
    }
    SymExpr newSize = size.substitute(varName, replacement);
    if (newSize != size) {
      return new SymArray("newarray", objectType, newSize, getKind());
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    if (size == null) {
      return this;
    }
    SymExpr newSize = size.substituteParam(idx, actual);
    if (newSize != size) {
      return new SymArray("newarray", objectType, newSize, getKind());
    }
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      if (size == null) {
        cachedToString = name + ":" + objectType;
      } else {
        cachedToString = "newarray(" + objectType + ")[" + size + "]";
      }
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return size != null && size.contains(varName);
  }
}
