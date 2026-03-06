package br.unb.cic.witup.analysis.symbolic;

public interface SymExprVisitor<T> {
  T visitBinOp(SymBinOp b);

  T visitConst(SymConst c);

  T visitFieldAccess(SymFieldAccess f);

  T visitStringConst(SymStringConst s);

  T visitVar(SymVar v);

  T visitVirtualInvoke(SymVirtualInvoke v);

  T visitNewArray(SymNewArray r);

  T visitArray(SymArray r);

  T visitArrayRef(SymArrayRef r);

  T visitLength(SymLength l);

  T visitCast(SymCast c);

  T visitInstanceOf(SymInstanceOf r);
}
