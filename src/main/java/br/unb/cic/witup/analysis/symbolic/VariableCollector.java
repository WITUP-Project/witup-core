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
    b.getLhs().accept(this);
    b.getRhs().accept(this);
    return null;
  }

  @Override
  public Void visitConst(final SymConst c) {
    return null;
  }

  @Override
  public Void visitIntConst(final SymIntConst i) {
    return null;
  }

  @Override
  public Void visitDoubleConst(final SymDoubleConst d) {
    return null;
  }

  @Override
  public Void visitFloatConst(final SymFloatConst f) {
    return null;
  }

  @Override
  public Void visitLongConst(final SymLongConstant l) {
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
  public Void visitStaticInvoke(final SymStaticInvoke s) {
    s.getArgs().forEach(arg -> arg.accept(this));
    return null;
  }

  @Override
  public Void visitInterfaceInvoke(final SymInterfaceInvoke i) {
    return null;
  }

  @Override
  public Void visitSpecialInvoke(final SymSpecialInvoke i) {
    return null;
  }

  @Override
  public Void visitDynamicInvoke(final SymDynamicInvoke d) {
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
    c.getOp().accept(this);
    return null;
  }

  @Override
  public Void visitInstanceOf(final SymInstanceOf r) {
    return null;
  }

  @Override
  public Void visitParamRef(final SymParamRef r) {
    return null;
  }

  @Override
  public Void visitNull(final SymNull n) {
    return null;
  }

  @Override
  public Void visitThisRef(final SymThisRef r) {
    return null;
  }

  @Override
  public Void visitCaughtException(final SymCaughtExceptionRef e) {
    return null;
  }

  @Override
  public Void visitNewRef(final SymNew n) {
    return null;
  }

  @Override
  public Void visitStaticFieldRef(final SymStaticFieldRef r) {
    return null;
  }

  @Override
  public Void visitNeg(final SymNeg n) {
    return n.getOperand().accept(this);
  }

  @Override
  public Void visitClassConst(final SymClassConst c) {
    return null;
  }

  @Override
  public Void visitITE(final SymITE ite) {
    ite.getCondition().accept(this);
    ite.getThenExpr().accept(this);
    ite.getElseExpr().accept(this);
    return null;
  }
}
