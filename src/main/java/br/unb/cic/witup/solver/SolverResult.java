package br.unb.cic.witup.solver;

import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.BoolValue;
import br.unb.cic.witup.solver.model.IntValue;
import br.unb.cic.witup.solver.model.ModelValue;
import br.unb.cic.witup.solver.model.StringValue;
import com.microsoft.z3.Status;
import java.util.Map;

public record SolverResult(
    String pathId,
    Status status,
    Map<String, ModelValue> modelValueMap) {

  public Status getStatus() {
    return status;
  }

  public String getPathId() {
    return pathId;
  }

  public Map<String, ModelValue> getModelValueMap() {
    return modelValueMap;
  }

  public boolean isSat() {
    return status == Status.SATISFIABLE;
  }

  public boolean isUnsat() {
    return status == Status.UNSATISFIABLE;
  }

  public int getInt(final String name) {
    ModelValue value = modelValueMap.get(name);
    if (value == null) {
      throw new IllegalStateException("No modelValueMap value for: " + name);
    }
    return value.getInt();
  }

  public boolean getBool(final String name) {
    ModelValue value = modelValueMap.get(name);
    if (value == null) {
      throw new IllegalStateException("No modelValueMap value for: " + name);
    }
    return value.getBool();
  }

  public ArrayValue getArray(final String name) {
    ModelValue value = modelValueMap.get(name);

    if (!(value instanceof ArrayValue av)) {
      throw new IllegalStateException(
          "Expected ArrayValue for "
              + name
              + ", got "
              + (value == null ? "null" : value.getClass()));
    }
    return av;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String name, final Class<T> klass) {
    ModelValue val = modelValueMap.get(name);
    if (val == null) {
      throw new IllegalStateException("No value for " + name);
    }
    Object primitive =
        switch (val) {
          case IntValue(int value) when klass == Integer.class -> value;
          case BoolValue(boolean value) when klass == Boolean.class -> value;
          case StringValue(String value) when klass == String.class -> value;
          default ->
              throw new IllegalStateException(
                  "Expected " + klass.getSimpleName() + " for " + name + ", got " + val.getClass());
        };
    return (T) primitive;
  }
}
