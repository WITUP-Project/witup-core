package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymKind;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SolverSerialiser {
  private final String methodId;

  public SolverSerialiser(final String methodId) {
    this.methodId = methodId;
  }

  public JSONObject serializeResolvedPaths(
      final List<List<ResolvedThrowCondition>> resolvedPaths,
      final Map<String, SymKind> symbolTypes) {
    JSONArray pathsArray = new JSONArray();

    for (int i = 0; i < resolvedPaths.size(); i++) {
      List<ResolvedThrowCondition> path = resolvedPaths.get(i);
      JSONArray conditionsArray = new JSONArray();

      for (ResolvedThrowCondition rc : path) {
        JSONObject obj = new JSONObject();
        obj.put("truthValue", rc.getTruthValue());
        obj.put("condition", rc.getNode().toString());
        conditionsArray.put(obj);
      }

      JSONObject pathObject = new JSONObject();
      // composite ID = method + index
      pathObject.put("pathId", methodId + "#" + i);
      pathObject.put("conditions", conditionsArray);

      pathsArray.put(pathObject);
    }

    JSONObject symbolsObj = new JSONObject();
    for (Map.Entry<String, SymKind> entry : symbolTypes.entrySet()) {
      symbolsObj.put(entry.getKey(), entry.getValue().name());
    }

    JSONObject request = new JSONObject();
    request.put("paths", pathsArray);
    request.put("symbolKinds", symbolsObj);
    return request;
  }
}
