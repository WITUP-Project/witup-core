package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Set;
import sootup.core.jimple.common.ref.JArrayRef;

public final class SymArrayRef extends SymExpr {
  private final SymExpr array;
  private final SymExpr index;
  private String cachedToString;
  private int cachedHashCode;

  public static SymExpr fromArrayRef(final JArrayRef r) {
    SymExpr base = fromJimple(r.getBase());
    SymExpr indexExpr = fromJimple(r.getIndex());
    return new SymArrayRef(base, indexExpr);
  }

  public SymArrayRef(final SymExpr array, final SymExpr index) {
    super(array.getKind(), maskOf(array, index));
    this.array = array;
    this.index = index;
  }

  public SymExpr getArray() {
    return array;
  }

  public SymExpr getIndex() {
    return index;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitArrayRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newArr = array.substituteParam(idx, actual);
    SymExpr newIndexRef = index.substituteParam(idx, actual);
    if (newArr != array || newIndexRef != index) {
      return new SymArrayRef(newArr, newIndexRef);
    }
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = array.toString() + "[" + index + "]";
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return array.contains(varName) || index.contains(varName);
  }

  @Override
  public void collectVarNames(final Set<String> vars) {
    array.collectVarNames(vars);
    index.collectVarNames(vars);
  }

  public SymKind kind() {
    if (array instanceof SymArray arr) {
      return arr.getKind();
    }
    return SymKind.OTHER;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymArrayRef symArrayRef)) {
      return false;
    }
    return array.equals(symArrayRef.array) && index.equals(symArrayRef.index);
  }

  @Override
  public int hashCode() {
    int h = cachedHashCode;
    if (h == 0) {
      final int prime = 31;
      h = prime * array.hashCode() + index.hashCode();
      cachedHashCode = h;
    }
    return h;
  }
}
