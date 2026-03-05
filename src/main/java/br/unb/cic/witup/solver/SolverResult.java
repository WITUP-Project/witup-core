package br.unb.cic.witup.solver;

import com.microsoft.z3.Context;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Status;

import java.util.Map;

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
    if (value == null) throw new IllegalStateException("No model value for: " + name);
    return value.getInt();
  }

  public boolean getBool(String name) {
    ModelValue value = model.get(name);
    if (value == null) throw new IllegalStateException("No model value for: " + name);
    return value.getBool();
  }

  public String getString(String name) {
    ModelValue value = model.get(name);
    if (value == null) throw new IllegalStateException("No model value for: " + name);
    return value.getString();
  }

  public ModelValue.ArrayValue getArray(String name) {
    ModelValue value = model.get(name);

    if (!(value instanceof ModelValue.ArrayValue av)) {
      throw new IllegalStateException("Expected ArrayValue for " + name + ", got " +
              (value == null ? "null" : value.getClass()));
    }
    return av;
  }

//  public ModelValue.ObjectValue getObject(String name) {
//    Expr<?> val = context.mkConst(name, context.mkUninterpretedSort(name + "_obj"));
//    Expr<?> eval = z3Model.eval(val, true);
//
//    return new ModelValue.ObjectValue(eval, z3Model, context);
//  }

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
}
