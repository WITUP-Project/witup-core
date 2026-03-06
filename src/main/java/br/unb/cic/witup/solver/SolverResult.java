package br.unb.cic.witup.solver;

import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.BoolValue;
import br.unb.cic.witup.solver.model.IntValue;
import br.unb.cic.witup.solver.model.ModelValue;
import br.unb.cic.witup.solver.model.StringValue;
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
      final String pathId,
      final Status status,
      final Map<String, ModelValue> model,
      final Context context,
      final Model z3Model) {

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

  public int getInt(final String name) {
    ModelValue value = model.get(name);
    if (value == null) {
      throw new IllegalStateException("No model value for: " + name);
    }
    return value.getInt();
  }

  public boolean getBool(final String name) {
    ModelValue value = model.get(name);
    if (value == null) {
      throw new IllegalStateException("No model value for: " + name);
    }
    return value.getBool();
  }

  public String getString(final String name) {
    ModelValue value = model.get(name);
    if (value == null) {
      throw new IllegalStateException("No model value for: " + name);
    }
    return value.getString();
  }

  public ArrayValue getArray(final String name) {
    ModelValue value = model.get(name);

    if (!(value instanceof ArrayValue av)) {
      throw new IllegalStateException(
          "Expected ArrayValue for "
              + name
              + ", got "
              + (value == null ? "null" : value.getClass()));
    }
    return av;
  }

  //  public ModelValue.ObjectValue getObject(String name) {
  //    Expr<?> val = context.mkConst(name, context.mkUninterpretedSort(name + "_obj"));
  //    Expr<?> eval = z3Model.eval(val, true);
  //
  //    return new ModelValue.ObjectValue(eval, z3Model, context);
  //  }

  public IntExpr getIntExpr(final String name) {
    return context.mkIntConst(name);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String name, final Class<T> cls) {
    ModelValue val = model.get(name);
    if (val == null) {
      throw new IllegalStateException("No value for " + name);
    }
    Object primitive =
        switch (val) {
          case IntValue(int value) when cls == Integer.class -> value;
          case BoolValue(boolean value) when cls == Boolean.class -> value;
          case StringValue(String value) when cls == String.class -> value;
          default ->
              throw new IllegalStateException(
                  "Expected " + cls.getSimpleName() + " for " + name + ", got " + val.getClass());
        };
    return (T) primitive;
  }
}
