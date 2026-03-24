package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
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
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;

public final class Z3SortDetector implements SymExprVisitor<Sort> {
  private final Context context;

  public Z3SortDetector(final Context context) {
    this.context = context;
  }

  @Override
  public Sort visitBinOp(final SymBinOp b) {
    return b.getLhs().accept(this);
  }

  @Override
  public Sort visitConst(final SymConst c) {
    return switch (c.getValue()) {
      case null -> context.getIntSort(); // null → opaque int
      case Double ignored -> context.getRealSort();
      case Float ignored -> context.getRealSort();
      default -> context.getIntSort();
    };
  }

  @Override
  public Sort visitIntConst(final SymIntConst c) {
    return context.getIntSort();
  }

  @Override
  public Sort visitDoubleConst(final SymDoubleConst d) {
    return context.getRealSort();
  }

  @Override
  public Sort visitFloatConst(final SymFloatConst f) {
    return context.getRealSort();
  }

  @Override
  public Sort visitLongConst(final SymLongConstant l) {
    return context.getIntSort();
  }

  @Override
  public Sort visitFieldAccess(final SymFieldAccess f) {
    return context.getIntSort();
  }

  @Override
  public Sort visitStringConst(final SymStringConst s) {
    return context.getStringSort();
  }

  @Override
  public Sort visitVar(final SymVar v) {
    // feels bad to compare typenames and then kinds.
    // need to unify this after splitting tests.
    String typeStr = v.getTypeName();
    if (typeStr != null && typeStr.endsWith("[]")) {
      String elementType = typeStr.substring(0, typeStr.length() - 2);
      Sort elementSort =
          switch (elementType) {
            case "int", "short", "byte", "long", "char" -> context.getIntSort();
            case "boolean" -> context.getBoolSort();
            case "float", "double" -> context.mkRealSort();
            case "java.lang.String" -> context.getStringSort();
            default -> context.mkUninterpretedSort("java.lang.Object");
          };
      return context.mkArraySort(context.getIntSort(), elementSort);
    }
    return switch (v.getKind()) {
      case BOOLEAN, BOOLEAN_METHOD -> context.getBoolSort();
      case STRING -> context.getStringSort();
      default -> context.getIntSort();
    };
  }

  @Override
  public Sort visitVirtualInvoke(final SymVirtualInvoke v) {
    return v.getKind() == SymKind.BOOLEAN_METHOD ? context.getBoolSort() : context.getIntSort();
  }

  @Override
  public Sort visitStaticInvoke(final SymStaticInvoke s) {
    return s.getKind() == SymKind.BOOLEAN_METHOD ? context.getBoolSort() : context.getIntSort();
  }

  @Override
  public Sort visitInterfaceInvoke(final SymInterfaceInvoke i) {
    return i.getKind() == SymKind.BOOLEAN_METHOD ? context.getBoolSort() : context.getIntSort();
  }

  @Override
  public Sort visitSpecialInvoke(final SymSpecialInvoke i) {
    return i.getKind() == SymKind.BOOLEAN_METHOD ? context.getBoolSort() : context.getIntSort();
  }

  @Override
  public Sort visitDynamicInvoke(final SymDynamicInvoke i) {
    return context.getIntSort();
  }

  //  @Override
  //  public Sort visitNewArray(final SymNewArray r) {
  //    // only int
  //    return context.mkArraySort(context.getIntSort(), context.getIntSort());
  //  }

  @Override
  public Sort visitArray(final SymArray symArray) {
    // Declare array sort based on element type
    Sort elemSort =
        switch (symArray.getKind()) {
          case INT -> context.getIntSort();
          case STRING -> context.getStringSort();
          case REAL -> context.getRealSort();
          case BOOLEAN -> context.getBoolSort();
          // we cannot anticipate user types so lift them all to objects
          case OBJECT -> context.mkUninterpretedSort("java.lang.Object");
          default -> context.getIntSort();
        };
    // array indices are always ints
    return context.mkArraySort(context.getIntSort(), elemSort);
  }

  @Override
  public Sort visitArrayRef(final SymArrayRef r) {
    // Return the element sort of the underlying array
    Sort arraySort = r.getArray().accept(this);
    if (!(arraySort instanceof ArraySort arrSort)) {
      throw new IllegalStateException(
          "Expected ArraySort for array base, got " + arraySort.getClass());
    }
    return arrSort.getRange(); // arr[i]
  }

  @Override
  public Sort visitNewMultiArray(final SymNewMultiArray e) {
    return context.getIntSort();
  }

  @Override
  public Sort visitLength(final SymLength l) {
    return context.getIntSort();
  }

  @Override
  public Sort visitCast(final SymCast c) {
    return context.getIntSort();
  }

  @Override
  public Sort visitInstanceOf(final SymInstanceOf r) {
    return context.getBoolSort();
  }

  @Override
  public Sort visitParamRef(final SymParamRef r) {
    return switch (r.getKind()) {
      case BOOLEAN, BOOLEAN_METHOD -> context.getBoolSort();
      case STRING -> context.getStringSort();
      default -> context.getIntSort();
    };
  }

  @Override
  public Sort visitNull(final SymNull n) {
    return context.getIntSort();
  }

  @Override
  public Sort visitThisRef(final SymThisRef r) {
    return context.mkUninterpretedSort(r.getType());
  }

  @Override
  public Sort visitCaughtException(final SymCaughtExceptionRef e) {
    return context.getBoolSort();
  }

  @Override
  public Sort visitNewRef(final SymNew n) {
    return context.getIntSort();
  }

  @Override
  public Sort visitStaticFieldRef(final SymStaticFieldRef r) {
    return r.getKind() == SymKind.BOOLEAN ? context.getBoolSort() : context.getIntSort();
  }

  @Override
  public Sort visitNeg(final SymNeg n) {
    return n.getOperand().accept(this);
  }

  @Override
  public Sort visitClassConst(final SymClassConst c) {
    return context.getIntSort();
  }

  @Override
  public Sort visitITE(final SymITE ite) {
    return ite.getThenExpr().accept(this);
  }
}
