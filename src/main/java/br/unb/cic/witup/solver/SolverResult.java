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

  public ModelValue.IntValue getInt(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.IntValue iv)) {
      throw new IllegalStateException("Expected IntValue for " + name + ", got " + value.getClass());
    }
    return iv;
  }

  public ModelValue.BoolValue getBool(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.BoolValue bv)) {
      throw new IllegalStateException("Expected BoolValue for " + name + ", got " + value.getClass());
    }
    return bv;
  }

  public ModelValue.StringValue getString(String name) {
    ModelValue value = model.get(name);
    if (!(value instanceof ModelValue.StringValue sv)) {
      throw new IllegalStateException("Expected StringValue for " + name + ", got " + value.getClass());
    }
    return sv;
  }
}