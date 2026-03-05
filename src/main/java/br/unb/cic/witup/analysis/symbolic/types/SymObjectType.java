package br.unb.cic.witup.analysis.symbolic.types;

import java.util.Map;

public final class SymObjectType {
  private final Map<String, SymKind> fields;

  public SymObjectType(Map<String, SymKind> fields) {
    this.fields = fields;
  }

  public SymKind getFieldKind(String field) {
    SymKind kind = fields.get(field);
    if (kind == null) {
      throw new IllegalStateException("Unknown field: " + field);
    }
    return kind;
  }
}
