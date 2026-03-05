package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.types.SymObjectType;
import com.microsoft.z3.Sort;

import java.util.HashMap;
import java.util.Map;

public final class SymTypeRegistry {

  private static final Map<Sort, SymObjectType> registry = new HashMap<>();

  public static void register(Sort sort, SymObjectType type) {
    registry.put(sort, type);
  }

  public static SymObjectType lookup(Sort sort) {
    SymObjectType t = registry.get(sort);
    if (t == null) {
      throw new IllegalStateException("No object type registered for sort " + sort);
    }
    return t;
  }
}
