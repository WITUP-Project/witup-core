package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymConst extends SymExpr {
  public static final SymConst TRUE = new SymConst(1, SymKind.BOOLEAN);
  public static final SymConst FALSE = new SymConst(0, SymKind.BOOLEAN);

  private final Object value;

  public SymConst(final Object value, final SymKind kind) {
    super(kind);
    this.value = value;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitConst(this);
  }

  public Object getValue() {
    return value;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    // Constants don't contain variables
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymConst)) {
      return false;
    }
    SymConst symConst = (SymConst) o;
    return value.equals(symConst.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
