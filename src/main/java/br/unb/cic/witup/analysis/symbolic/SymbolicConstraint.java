package br.unb.cic.witup.analysis.symbolic;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SymbolicConstraint {
  private final SymExpr symExpr;
  private final boolean truthValue;

  public SymbolicConstraint(final SymExpr symExpr, final boolean truthValue) {
    this.symExpr = symExpr;
    this.truthValue = truthValue;
  }

  public SymExpr getSymExpr() {
    return symExpr;
  }

  public boolean getTruthValue() {
    return truthValue;
  }

  public static JSONArray serializeResolvedPaths(final List<List<SymbolicConstraint>> paths) {

    JSONArray allPaths = new JSONArray();

    for (List<SymbolicConstraint> path : paths) {
      JSONArray jsonPath = new JSONArray();

      for (SymbolicConstraint rc : path) {
        JSONObject obj = new JSONObject();
        obj.put("truthValue", rc.getTruthValue());
        obj.put("condition", rc.getSymExpr().toString());
        jsonPath.put(obj);
      }

      allPaths.put(jsonPath);
    }

    return allPaths;
  }
}
