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

  T visitNewMultiArray(SymNewMultiArray r);

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

  T visitClassConst(SymClassConst c);

  T visitITE(SymITE i);
}
