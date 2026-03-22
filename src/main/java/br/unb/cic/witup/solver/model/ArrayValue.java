package br.unb.cic.witup.solver.model;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_SEQ_SORT;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.Sort;

public record ArrayValue(ArrayExpr<IntSort, ?> arrayExpr, Model model, Context ctx)
    implements ModelValue {

  public ModelValue get(final String indexName) {
    return get(ctx.mkIntConst(indexName));
  }

  public ModelValue get(final int index) {
    return get(ctx.mkInt(index));
  }

  public ModelValue get(final IntExpr indexExpr) {
    Expr<?> val = model.eval(ctx.mkSelect(arrayExpr, indexExpr), true);
    Sort rangeSort = ((ArraySort<?, ?>) arrayExpr.getSort()).getRange();

    return switch (rangeSort.getSortKind()) {
      case Z3_INT_SORT -> parseIntValue(val);
      case Z3_SEQ_SORT -> new StringValue(val.getString());
      case Z3_BOOL_SORT -> new BoolValue(val.isTrue());
      case Z3_UNINTERPRETED_SORT -> resolveUninterpreted(arrayExpr, indexExpr, val);
      default -> ModelValue.fromExpr(val, model, ctx);
    };
  }

  private IntValue parseIntValue(final Expr<?> val) {
    try {
      return new IntValue(Integer.parseInt(val.toString()));
    } catch (NumberFormatException e) {
      return new IntValue(0); // unconstrained — Z3 picked an arbitrary value
    }
  }

  private ModelValue resolveUninterpreted(
      final ArrayExpr<IntSort, ?> arrExpr, final IntExpr indexExpr, final Expr<?> val) {
    Expr<?> selectExpr = ctx.mkSelect(arrExpr, indexExpr);
    // strengthen me
    String asStrKey = selectExpr + "_as_str";
    Expr<?> strConst = ctx.mkConst(asStrKey, ctx.getStringSort());
    Expr<?> strVal = model.eval(strConst, true);
    if (strVal.getSort().getSortKind() == Z3_SEQ_SORT && !strVal.getString().isEmpty()) {
      return new StringValue(strVal.getString());
    }
    return new ObjectValue(val, model, ctx);
  }
}
