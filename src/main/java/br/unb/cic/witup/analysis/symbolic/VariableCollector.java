package br.unb.cic.witup.analysis.symbolic;

import java.util.HashSet;
import java.util.Set;

public final class VariableCollector implements SymExprVisitor<Void> {
  private final Set<String> variables = new HashSet<>();

  public Set<String> collect(final SymExpr expr) {
    expr.accept(this);
    return variables;
  }

  @Override
  public Void visitBinOp(final SymBinOp b) {
    b.getLeft().accept(this);
    b.getRight().accept(this);
    return null;
  }

  @Override
  public Void visitConst(final SymConst c) {
    return null;
  }

  @Override
  public Void visitFieldAccess(final SymFieldAccess f) {
    f.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitStringConst(final SymStringConst c) {
    return null;
  }

  @Override
  public Void visitVar(final SymVar v) {
    variables.add(v.getName());
    return null;
  }

  @Override
  public Void visitVirtualInvoke(final SymVirtualInvoke inv) {
    inv.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitArray(final SymArray r) {
    return null;
  }

  @Override
  public Void visitArrayRef(final SymArrayRef r) {
    r.getArray().accept(this);
    variables.add(r.toString());
    r.getIndex().accept(this);
    return null;
  }

  @Override
  public Void visitLength(final SymLength l) {
    l.getOp().accept(this);
    return null;
  }

  @Override
  public Void visitCast(final SymCast c) {
    return null;
  }

  @Override
  public Void visitInstanceOf(final SymInstanceOf r) {
    return null;
  }

  @Override
  public Void visitParamRef(final SymParam r) {
    return null;
  }
}
