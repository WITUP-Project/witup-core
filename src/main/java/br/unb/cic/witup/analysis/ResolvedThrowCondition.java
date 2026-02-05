package br.unb.cic.witup.analysis;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ResolvedThrowCondition {
  private SymExpr node;
  private boolean truthValue;

  public ResolvedThrowCondition(final SymExpr node, final boolean truthValue) {
    this.node = node;
    this.truthValue = truthValue;
  }

  public SymExpr getNode() {
    return node;
  }

  public boolean getTruthValue() {
    return truthValue;
  }

  public static JSONArray serializeResolvedPaths(final List<List<ResolvedThrowCondition>> paths) {

    JSONArray allPaths = new JSONArray();

    for (List<ResolvedThrowCondition> path : paths) {
      JSONArray jsonPath = new JSONArray();

      for (ResolvedThrowCondition rc : path) {
        JSONObject obj = new JSONObject();
        obj.put("truthValue", rc.getTruthValue());
        obj.put("condition", rc.getNode().toString());
        jsonPath.put(obj);
      }

      allPaths.put(jsonPath);
    }

    return allPaths;
  }
}
