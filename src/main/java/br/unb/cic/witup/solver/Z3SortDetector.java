package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.SymArray;
import br.unb.cic.witup.analysis.symbolic.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymCast;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymField;
import br.unb.cic.witup.analysis.symbolic.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.SymKind;
import br.unb.cic.witup.analysis.symbolic.SymLength;
import br.unb.cic.witup.analysis.symbolic.SymNewArray;
import br.unb.cic.witup.analysis.symbolic.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.SymVar;
import br.unb.cic.witup.analysis.symbolic.SymVirtualInvoke;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Sort;

public final class Z3SortDetector implements SymExprVisitor<Sort> {
  private final Context context;

  public Z3SortDetector(final Context context) {
    this.context = context;
  }

  @Override
  public Sort visitBinOp(final SymBinOp b) {
    return b.getLeft().accept(this);
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
  public Sort visitField(final SymField f) {
    return context.getIntSort();
  }

  @Override
  public Sort visitStringConst(final SymStringConst s) {
    return context.getStringSort();
  }

  @Override
  public Sort visitVar(final SymVar v) {
    return switch (v.kind()) {
      case BOOLEAN, BOOLEAN_METHOD -> context.getBoolSort();
      case STRING -> context.getStringSort();
      default -> context.getIntSort();
    };
  }

  @Override
  public Sort visitVirtualInvoke(final SymVirtualInvoke v) {
    return v.kind() == SymKind.BOOLEAN_METHOD ? context.getBoolSort() : context.getIntSort();
  }

  @Override
  public Sort visitArrayRef(final SymArrayRef r) {
    // Return the element sort of the underlying array
    Sort arraySort = r.getArray().accept(this);
    if (!(arraySort instanceof ArraySort arrSort)) {
      throw new IllegalStateException("Expected ArraySort for array base, got " + arraySort.getClass());
    }
    return arrSort.getRange(); // arr[i]
  }

  @Override
  public Sort visitLength(final SymLength l) {
    return context.getIntSort();
  }

  @Override
  public Sort visitNewArray(final SymNewArray r) {
    // only int
    return context.mkArraySort(context.getIntSort(), context.getIntSort());
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
  public Sort visitArray(final SymArray symArray) {
    // Declare array sort based on element type
    Sort elemSort = switch (symArray.getElementKind()) {
      case INT -> context.getIntSort();
      case STRING -> context.getStringSort();
      case REAL -> context.getRealSort();
      case BOOLEAN -> context.getBoolSort();
      case OBJECT -> context.getStringSort();
      default -> context.getIntSort();
    };
    // array indices are always ints
    return context.mkArraySort(context.getIntSort(), elemSort);
  }
}
