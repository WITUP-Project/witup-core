package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.ref.JThisRef;

public final class SymThisRef extends SymExpr {
  private final String type;
  private final String cachedToString;

  public SymThisRef(final JThisRef r) {
    // The receiver is formal index -1 by buildFormals' convention. Without it, compound
    // parents short-circuit on containsParam(-1) and a callee's `this.field` predicate
    // reaches the caller still talking about 'this
    super(SymKind.OTHER, 1L << SymParamRef.THIS_INDEX);
    type = r.getType().toString();
    this.cachedToString = "this";
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitThisRef(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    return idx == SymParamRef.THIS_INDEX ? actual : this;
  }

  @Override
  public String toString() {
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymThisRef symThisRef)) {
      return false;
    }
    return type.equals(symThisRef.type);
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }
}
