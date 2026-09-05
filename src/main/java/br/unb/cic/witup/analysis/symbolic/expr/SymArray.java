package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.common.expr.JNewArrayExpr;

/** Represents an allocated array */
public final class SymArray extends SymExpr {
  private final String name;
  private final String objectType;
  private final SymExpr size;
  private String cachedToString;

  public SymArray(final JNewArrayExpr newArrExpr) {
    this(
        "newarray",
        newArrExpr.getType().toString(),
        fromJimple(newArrExpr.getSize()),
        fromJimpleType(newArrExpr.getType()));
  }

  private SymArray(
      final String name, final String objectType, final SymExpr size, final SymKind kind) {
    super(kind, maskOf(size));
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

  @Override
  public void collectVarNames(final Set<String> vars) {
    if (size != null) {
      size.collectVarNames(vars);
    }
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    if (size == null) {
      return this;
    }
    SymExpr newSize = size.resolveWith(env);
    return (newSize != size) ? new SymArray("newarray", objectType, newSize, getKind()) : this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymArray symArray)) {
      return false;
    }
    return name.equals(symArray.name)
        && objectType.equals(symArray.objectType)
        && java.util.Objects.equals(size, symArray.size);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * (prime * name.hashCode() + objectType.hashCode())
        + (size == null ? 0 : size.hashCode());
  }
}
