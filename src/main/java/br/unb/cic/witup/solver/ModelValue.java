package br.unb.cic.witup.solver;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.enumerations.Z3_sort_kind;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_SEQ_SORT;

public sealed interface ModelValue
    permits ModelValue.IntValue, ModelValue.BoolValue, ModelValue.StringValue, ModelValue.ArrayValue {
  default int getInt() {
    if (this instanceof IntValue iv) {
      return iv.value();
    }
    throw new IllegalStateException("Expected IntValue, got " + getClass());
  }

  default boolean getBool() {
    if (this instanceof BoolValue bv) {
      return bv.value();
    }
    throw new IllegalStateException("Expected BoolValue, got " + getClass());
  }

  default String getString() {
    if (this instanceof StringValue sv) {
      return sv.value();
    }
    throw new IllegalStateException("Expected StringValue, got " + getClass());
  }

  record IntValue(int value) implements ModelValue {}

  record BoolValue(boolean value) implements ModelValue {}

  record StringValue(String value) implements ModelValue {}

  public record ArrayValue(ArrayExpr<IntSort, ?> arrayExpr, Model model, Context ctx)
          implements ModelValue {

    public ModelValue get(IntExpr indexExpr) {
      // use the injected ctx here
      Expr<?> val = model.eval(ctx.mkSelect(arrayExpr, indexExpr), true);
      Z3_sort_kind sortKind = val.getSort().getSortKind();

      if (val.isIntNum()) {
        return new ModelValue.IntValue(((IntNum) val).getInt());
      }
      if (val.isBool()) {
        return new ModelValue.BoolValue(val.isTrue());
      }
      if (sortKind == Z3_SEQ_SORT) {
        return new ModelValue.StringValue(val.getString());
      }
      throw new IllegalStateException("Unsupported array element: " + val.getSort());
    }
  }
}
