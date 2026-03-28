package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.basic.Local;

public final class SymVar extends SymExpr {
  private final String name;
  private final String typeName;

  public SymVar(final Local l) {
    super(fromJimpleType(l.getType()));
    this.name = l.getName();
    this.typeName = l.getType().toString();
  }

  private SymVar(final String name, final SymKind kind) {
    super(kind);
    this.name = name;
    this.typeName = kind.toString();
  }

  public static SymVar fresh(final String name, final SymKind kind) {
    return new SymVar(name, kind);
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitVar(this);
  }

  public String getName() {
    return name;
  }

  public String getTypeName() {
    return typeName;
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
  public void collectVarNames(final Set<String> vars) {
    vars.add(name);
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    SymExpr replacement = env.remove(name);
    if (replacement == null) {
      return this;
    }
    SymExpr result = replacement.resolveWith(env);
    env.put(name, replacement);
    return result;
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
    if (!(o instanceof SymVar symVar)) {
      return false;
    }
    return name.equals(symVar.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
