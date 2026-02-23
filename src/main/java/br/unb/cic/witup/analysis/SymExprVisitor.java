package br.unb.cic.witup.analysis;

public interface SymExprVisitor<T> {
  T visitBinOp(SymBinOp b);
  T visitConst(SymConst c);
  T visitField(SymField f);
  T visitStringConst(SymStringConst s);
  T visitVar(SymVar v);
  T visitVirtualInvoke(SymVirtualInvoke v);
}
