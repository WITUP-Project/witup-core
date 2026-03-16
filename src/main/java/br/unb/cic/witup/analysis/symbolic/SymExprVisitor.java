package br.unb.cic.witup.analysis.symbolic;

public interface SymExprVisitor<T> {
  T visitBinOp(SymBinOp b);

  T visitConst(SymConst c);

  T visitIntConst(SymIntConst i);

  T visitDoubleConst(SymDoubleConst d);

  T visitFloatConst(SymFloatConst f);

  T visitLongConst(SymLongConstant l);

  T visitFieldAccess(SymFieldAccess f);

  T visitStringConst(SymStringConst s);

  T visitVar(SymVar v);

  T visitVirtualInvoke(SymVirtualInvoke v);

  T visitStaticInvoke(SymStaticInvoke i);

  T visitInterfaceInvoke(SymInterfaceInvoke i);

  T visitSpecialInvoke(SymSpecialInvoke i);

  T visitDynamicInvoke(SymDynamicInvoke i);

  T visitArray(SymArray r);

  T visitArrayRef(SymArrayRef r);

  T visitLength(SymLength l);

  T visitCast(SymCast c);

  T visitInstanceOf(SymInstanceOf r);

  T visitParamRef(SymParamRef p);

  T visitNull(SymNull n);

  T visitThisRef(SymThisRef r);

  T visitCaughtException(SymCaughtExceptionRef e);

  T visitNewRef(SymNew n);

  T visitStaticFieldRef(SymStaticFieldRef r);

  T visitNeg(SymNeg n);
}
