package br.unb.cic.witup.analysis;

import java.util.HashSet;
import java.util.Set;

public class VariableCollector implements SymExprVisitor<Void> {
  private final Set<String> variables = new HashSet<>();

  public Set<String> collect(SymExpr expr) {
    expr.accept(this);
    return variables;
  }

  @Override
  public Void visitVar(SymVar v) {
    variables.add(v.getName());
    return null;
  }

  @Override
  public Void visitConst(SymConst c) {
    return null;
  }

  @Override
  public Void visitStringConst(SymStringConst c) {
    return null;
  }

  @Override
  public Void visitBinOp(SymBinOp b) {
    b.getLeft().accept(this);
    b.getRight().accept(this);
    return null;
  }

  @Override
  public Void visitField(SymField f) {
    f.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitVirtualInvoke(SymVirtualInvoke inv) {
    inv.getBase().accept(this);
    return null;
  }
}
