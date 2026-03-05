package br.unb.cic.witup.solver;

import com.microsoft.z3.Status;

import java.util.Map;

public record SolverResult(
        String pathId,
        Status status,
        Map<String, ModelValue> model
) {
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

  public int getInt(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.IntValue iv)) {
      throw new IllegalStateException("Expected IntValue for " + name + ", got " + value.getClass());
    }
    return iv.getValue();
  }

  public boolean getBool(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.BoolValue bv)) {
      throw new IllegalStateException("Expected BoolValue for " + name + ", got " + value.getClass());
    }
    return bv.getValue();
  }

  public String getString(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.StringValue sv)) {
      throw new IllegalStateException("Expected StringValue for " + name + ", got " + value.getClass());
    }
    return sv.getValue();
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
      primitive = iv.getValue();
    } else if (cls == Boolean.class && val instanceof ModelValue.BoolValue bv) {
      primitive = bv.getValue();
    } else if (cls == String.class && val instanceof ModelValue.StringValue sv) {
      primitive = sv.getValue();
    } else {
      throw new IllegalStateException("Expected " + cls.getSimpleName() + " for " + name + ", got " + val.getClass());
    }
    return (T) primitive;
  }
}