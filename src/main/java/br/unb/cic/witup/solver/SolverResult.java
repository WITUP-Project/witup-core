package br.unb.cic.witup.solver;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.Status;
import java.util.Map;

// (String pathId, Status status, Map<String, ModelValue> model)
public final class SolverResult {
  private final String pathId;
  private final Status status;
  private final Map<String, ModelValue> model;

  private final Context context;
  private final Model z3Model;

  public SolverResult(
          String pathId,
          Status status,
          Map<String, ModelValue> model,
          Context context,
          Model z3Model) {

    this.pathId = pathId;
    this.status = status;
    this.model = model;
    this.context = context;
    this.z3Model = z3Model;
  }

  public boolean isSat() {
    return status == Status.SATISFIABLE;
  }

  public boolean isUnsat() {
    return status == Status.UNSATISFIABLE;
  }

  public Status getStatus() {
    return status;
  }

  public String getPathId() {
    return pathId;
  }

  public Map<String, ModelValue> getModel() {
    return model;
  }

  public Context getContext() {
    return context;
  }

  public Model getZ3Model() {
    return z3Model;
  }

  public int getInt(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.IntValue iv)) {
      throw new IllegalStateException(
          "Expected IntValue for " + name + ", got " + value.getClass());
    }
    return iv.value();
  }

  public boolean getBool(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.BoolValue bv)) {
      throw new IllegalStateException(
          "Expected BoolValue for " + name + ", got " + value.getClass());
    }
    return bv.value();
  }

  public String getString(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.StringValue sv)) {
      throw new IllegalStateException(
          "Expected StringValue for " + name + ", got " + value.getClass());
    }
    return sv.value();
  }

  public ModelValue.ArrayValue getArray(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.ArrayValue av)) {
      throw new IllegalStateException(
              "Expected ArrayValue for " + name + ", got " + (value == null ? "null" : value.getClass())
      );
    }
    return av;
  }

  public IntExpr getIntExpr(String name) {
    return context.mkIntConst(name);
  }

  // this makes the tests look cleaner, but might be too much
  // consider removing
  @SuppressWarnings("unchecked")
  public <T> T get(String name, Class<T> cls) {
    ModelValue val = model.get(name);
    if (val == null) {
      throw new IllegalStateException("No value for " + name);
    }
    Object primitive;
    if (cls == Integer.class && val instanceof ModelValue.IntValue iv) {
      primitive = iv.value();
    } else if (cls == Boolean.class && val instanceof ModelValue.BoolValue bv) {
      primitive = bv.value();
    } else if (cls == String.class && val instanceof ModelValue.StringValue sv) {
      primitive = sv.value();
    } else {
      throw new IllegalStateException(
          "Expected " + cls.getSimpleName() + " for " + name + ", got " + val.getClass());
    }
    return (T) primitive;
  }

  public ModelValue evalExpr(Expr<?> expr) {

    // I wonder if we can assume it always exists
    if (z3Model == null) {
      throw new IllegalStateException("Cannot evaluate expression: model is null (UNSAT result)");
    }

    Expr<?> value = z3Model.eval(expr, true);

    if (value.isIntNum()) {
      return new ModelValue.IntValue(((IntNum) value).getInt());
    }

    if (value.isBool()) {
      return new ModelValue.BoolValue(value.isTrue());
    }

    if (value.isString()) {
      return new ModelValue.StringValue(value.getString());
    }

    throw new IllegalStateException("Unsupported model value: " + value);
  }
}
