package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;

public final class SymVar extends SymExpr {
  private final String name;
  private final SymKind kind;

  public SymVar(final String name, final SymKind kind) {
    this.name = name;
    this.kind = kind;
  }

  // DEPRECATED
  public SymVar(final String name) {
    this(name, SymKind.OTHER);
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitVar(this);
  }

  public String getName() {
    return name;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    if (this.name.equals(varName)) {
      return replacement;
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return this.name.equals(varName);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymVar)) {
      return false;
    }
    SymVar symVar = (SymVar) o;
    return name.equals(symVar.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public SymKind kind() {
    return kind;
  }
}
