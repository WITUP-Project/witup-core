package br.unb.cic.witup.solver.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.enumerations.Z3_sort_kind;

public sealed interface ModelValue
    permits IntValue, BoolValue, StringValue, ArrayValue, ObjectValue {

  @JsonValue
  default String jsonValue() {
    return toString();
  }

  default ModelValue getField(final String fieldName) {
    throw new IllegalStateException("Not an object: " + this.getClass());
  }

  default int getInt() {
    if (this instanceof IntValue(final int value)) {
      return value;
    }
    throw new IllegalStateException("Expected IntValue, got " + getClass());
  }

  default boolean getBool() {
    if (this instanceof BoolValue(final boolean value)) {
      return value;
    }
    throw new IllegalStateException("Expected BoolValue, got " + getClass());
  }

  default String getString() {
    if (this instanceof StringValue(final String value)) {
      return value;
    }
    throw new IllegalStateException("Expected StringValue, got " + getClass());
  }

  static ModelValue fromExpr(final Expr<?> val, final Model model, final Context ctx) {
    if (val == null) {
      throw new IllegalStateException("Cannot convert null Z3 Expr");
    }

    Z3_sort_kind kind = val.getSort().getSortKind();

    switch (kind) {
      case Z3_INT_SORT:
        if (val instanceof IntNum num) {
          return new IntValue(num.getBigInteger().intValue());
        } else {
          try {
            return new IntValue(Integer.parseInt(val.toString()));
          } catch (NumberFormatException e) {
            return new IntValue(0);
          }
        }

      case Z3_BOOL_SORT:
        return new BoolValue(val.isTrue());

      case Z3_SEQ_SORT:
        return new StringValue(val.getString());

      case Z3_ARRAY_SORT:
        // cast is safe as we have already verified the sort
        return new ArrayValue((ArrayExpr<IntSort, ?>) val, model, ctx);

      case Z3_UNINTERPRETED_SORT:
        String s = val.toString();
        // symbolic int
        try {
          int v = Integer.parseInt(s);
          return new IntValue(v);
        } catch (NumberFormatException ignored) {
        }
        // symbolic boolean
        if ("true".equals(s)) {
          return new BoolValue(true);
        }
        if ("false".equals(s)) {
          return new BoolValue(false);
        }
        // fallback: treat as object
        return new ObjectValue(val, model, ctx);

      default:
        throw new IllegalStateException("Unsupported sort: " + kind);
    }
  }
}
