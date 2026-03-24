package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.expr.SymArray;
import br.unb.cic.witup.analysis.symbolic.expr.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymCast;
import br.unb.cic.witup.analysis.symbolic.expr.SymCaughtExceptionRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymClassConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymDoubleConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymDynamicInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymFieldAccess;
import br.unb.cic.witup.analysis.symbolic.expr.SymFloatConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymITE;
import br.unb.cic.witup.analysis.symbolic.expr.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymInterfaceInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymLength;
import br.unb.cic.witup.analysis.symbolic.expr.SymLongConstant;
import br.unb.cic.witup.analysis.symbolic.expr.SymNeg;
import br.unb.cic.witup.analysis.symbolic.expr.SymNew;
import br.unb.cic.witup.analysis.symbolic.expr.SymNewMultiArray;
import br.unb.cic.witup.analysis.symbolic.expr.SymNull;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymSpecialInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticFieldRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymThisRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.expr.SymVirtualInvoke;
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
  public Void visitNewMultiArray(final SymNewMultiArray e) {
    e.getSizes().forEach(s -> s.accept(this));
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
