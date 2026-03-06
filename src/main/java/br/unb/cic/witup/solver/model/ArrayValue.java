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

  public ModelValue get(final IntExpr indexExpr) {
    Expr<?> val = model.eval(ctx.mkSelect(arrayExpr, indexExpr), true);
    Sort rangeSort = ((ArraySort) arrayExpr.getSort()).getRange();

    System.out.println(
        "ArrayValue.get: arrayExpr="
            + arrayExpr
            + " rangeSort="
            + rangeSort
            + " kind="
            + rangeSort.getSortKind());

    return switch (rangeSort.getSortKind()) {
      case Z3_INT_SORT -> {
        try {
          yield new IntValue(Integer.parseInt(val.toString()));
        } catch (NumberFormatException e) {
          yield new IntValue(0); // unconstrained — Z3 picked an arbitrary value
        }
      }
      case Z3_SEQ_SORT -> new StringValue(val.getString());
      case Z3_BOOL_SORT -> new BoolValue(val.isTrue());
      case Z3_UNINTERPRETED_SORT -> {
        Expr<?> selectExpr = ctx.mkSelect(arrayExpr, indexExpr);
        String asStrKey = selectExpr + "_as_str";
        Expr<?> strConst = ctx.mkConst(asStrKey, ctx.getStringSort());
        Expr<?> strVal = model.eval(strConst, true);
        if (strVal.getSort().getSortKind() == Z3_SEQ_SORT && !strVal.getString().isEmpty()) {
          yield new StringValue(strVal.getString());
        }
        yield new ObjectValue(val, model, ctx);
      }
      default -> ModelValue.fromExpr(val, model, ctx);
    };
  }
}
