package br.unb.cic.witup.solver;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_SEQ_SORT;

import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.Sort;
import com.microsoft.z3.enumerations.Z3_sort_kind;

public sealed interface ModelValue
    permits ModelValue.IntValue,
        ModelValue.BoolValue,
        ModelValue.StringValue,
        ModelValue.ArrayValue,
        ModelValue.ObjectValue {

  default ModelValue getField(String fieldName) {
    throw new IllegalStateException("Not an object: " + this.getClass());
  }

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

  static ModelValue fromExpr(Expr<?> val, Model model, Context ctx) {
    if (val == null) throw new IllegalStateException("Cannot convert null Z3 Expr");

    Z3_sort_kind kind = val.getSort().getSortKind();

    switch (kind) {
      case Z3_INT_SORT:
        if (val instanceof IntNum num) {
          return new IntValue(num.getInt());
        } else {
          // symbolic int: we can still treat as IntValue
          return new IntValue(Integer.parseInt(val.toString()));
        }

      case Z3_BOOL_SORT:
        return new BoolValue(val.isTrue());

      case Z3_SEQ_SORT:
        return new StringValue(val.getString());

      case Z3_ARRAY_SORT:
        return new ArrayValue((ArrayExpr<IntSort, ?>) val, model, ctx);

      case Z3_UNINTERPRETED_SORT:
        {
          String s = val.toString();
          // symbolic int detection
          try {
            int v = Integer.parseInt(s);
            return new IntValue(v);
          } catch (NumberFormatException ignored) {
          }
          // symbolic boolean detection
          if ("true".equals(s)) return new BoolValue(true);
          if ("false".equals(s)) return new BoolValue(false);
          // fallback: treat as object
          return new ObjectValue(val, model, ctx);
        }

      default:
        throw new IllegalStateException("Unsupported sort: " + kind);
    }
  }

  record IntValue(int value) implements ModelValue {}

  record BoolValue(boolean value) implements ModelValue {}

  record StringValue(String value) implements ModelValue {}

  public record ArrayValue(ArrayExpr<IntSort, ?> arrayExpr, Model model, Context ctx)
      implements ModelValue {

    public ModelValue get(IntExpr indexExpr) {
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
          String asStrKey = selectExpr.toString() + "_as_str";
          Expr<?> strConst = ctx.mkConst(asStrKey, ctx.getStringSort());
          Expr<?> strVal = model.eval(strConst, true);
          if (strVal.getSort().getSortKind() == Z3_SEQ_SORT && !strVal.getString().isEmpty()) {
            yield new StringValue(strVal.getString());
          }
          yield new ObjectValue(val, model, ctx);
        }
        default -> fromExpr(val, model, ctx);
      };
    }
  }

  final class ObjectValue implements ModelValue {
    private final Expr<?> objExpr;
    private final Model model;
    private final Context ctx;

    //    private final SymObjectType symType;

    public ObjectValue(Expr<?> objExpr, Model model, Context ctx) {
      this.objExpr = objExpr;
      this.model = model;
      this.ctx = ctx;
      //      this.symType = symType;
    }

    @Override
    public ModelValue getField(String fieldName) {
      // Find the field_<name> function in the model and apply it to this object
      for (FuncDecl<?> decl : model.getDecls()) {
        if (decl.getName().toString().equals("field_" + fieldName) && decl.getArity() == 1) {
          Expr<?> applied = ctx.mkApp(decl, objExpr);
          Expr<?> evaluated = model.eval(applied, true);
          System.out.println(
              "getField " + fieldName + ": objExpr=" + objExpr + " evaluated=" + evaluated);
          return fromExpr(evaluated, model, ctx);
        }
      }
      throw new IllegalStateException("No field function for: " + fieldName);
    }
  }
}
