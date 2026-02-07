package br.unb.cic.witup.analysis;

public final class SymVar extends SymExpr {
  private final String name;

  public SymVar(final String name) {
    this.name = name;
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
    return SymKind.OTHER;
  }
}
