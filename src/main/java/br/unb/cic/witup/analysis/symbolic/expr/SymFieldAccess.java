package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.common.ref.JInstanceFieldRef;

public final class SymFieldAccess extends SymExpr {
  private final SymExpr base; // e.g., "this" or another object
  private final String fieldName; // e.g., "radius"
  private String cachedToString;
  private int cachedHashCode;

  public SymFieldAccess(final SymExpr base, final JInstanceFieldRef r) {
    super(fromJimpleType(r.getType()));
    this.base = base;
    this.fieldName = r.getFieldSignature().getName();
  }

  private SymFieldAccess(final SymExpr value, final String fieldName, final SymKind kind) {
    super(kind);
    this.fieldName = fieldName;
    this.base = value;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitFieldAccess(this);
  }

  public SymExpr getBase() {
    return base;
  }

  public String getFieldName() {
    return fieldName;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newBase = base.substitute(varName, replacement);
    if (newBase != base) {
      return new SymFieldAccess(newBase, fieldName, getKind());
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newBase = base.substituteParam(idx, actual);
    if (newBase != base) {
      return new SymFieldAccess(newBase, fieldName, getKind());
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName);
  }

  @Override
  public void collectVarNames(final Set<String> vars) {
    base.collectVarNames(vars);
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    SymExpr newBase = base.resolveWith(env);
    return (newBase != base) ? new SymFieldAccess(newBase, fieldName, getKind()) : this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = base.toString() + "." + fieldName;
    }
    return cachedToString;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymFieldAccess symFieldAccess)) {
      return false;
    }
    return base.equals(symFieldAccess.base) && fieldName.equals(symFieldAccess.fieldName);
  }

  @Override
  public int hashCode() {
    int h = cachedHashCode;
    if (h == 0) {
      final int prime = 31;
      h = prime * base.hashCode() + fieldName.hashCode();
      cachedHashCode = h;
    }
    return h;
  }
}
