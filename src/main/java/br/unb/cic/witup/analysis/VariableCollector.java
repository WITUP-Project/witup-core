package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymExpr;
import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymField;
import br.unb.cic.witup.analysis.symbolic.SymLength;
import br.unb.cic.witup.analysis.symbolic.SymNewArray;
import br.unb.cic.witup.analysis.symbolic.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.SymVar;
import br.unb.cic.witup.analysis.symbolic.SymVirtualInvoke;

import java.util.HashSet;
import java.util.Set;

public final class VariableCollector implements SymExprVisitor<Void> {
  private final Set<String> variables = new HashSet<>();

  public Set<String> collect(final SymExpr expr) {
    expr.accept(this);
    return variables;
  }

  @Override
  public Void visitVar(final SymVar v) {
    variables.add(v.getName());
    return null;
  }

  @Override
  public Void visitConst(final SymConst c) {
    return null;
  }

  @Override
  public Void visitStringConst(final SymStringConst c) {
    return null;
  }

  @Override
  public Void visitBinOp(final SymBinOp b) {
    b.getLeft().accept(this);
    b.getRight().accept(this);
    return null;
  }

  @Override
  public Void visitField(final SymField f) {
    f.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitVirtualInvoke(final SymVirtualInvoke inv) {
    inv.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitArrayRef(SymArrayRef r) {
    r.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitLength(SymLength l) {
    l.getOp().accept(this);
    return null;
  }

  @Override
  public Void visitNewArray(SymNewArray r) {
    return null;
  }
}
