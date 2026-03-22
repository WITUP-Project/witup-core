package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymConst extends SymExpr {
  private final Object value;
  private String cachedToString;


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
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return false;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = value.toString();
    }
    return cachedToString;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymConst symConst)) {
      return false;
    }
    return value.equals(symConst.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
