package br.unb.cic.witup;

import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodParts;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a method's observable exception paths into the pandas-friendly rows
 */
public final class SummaryRowBuilder {

  private SummaryRowBuilder() {
    throw new UnsupportedOperationException(SummaryRowBuilder.class.getSimpleName());
  }

  /**
   * Builds the rows for one method, collapsing paths that describe the same observable flow.
   * two paths are the same flow when they agree on (exceptionType, throwSiteKind, constraints).
   * The call chain that reached the flow is not part of that identity.
   * a caller can arrive at one predicate through several helpers.
   * Keying on provenance therefore manufactures duplicates that inflate match count
   * throwSiteKind is part of the identity. The first occurrence is the one emitted
   *
   * @param artifact jar file name, echoed into every row
   * @param methodSig SootUp signature of the method being reported
   * @param paths the walker's observable paths, in walker order
   * @param pathIdToStatus solver verdicts by pathId
   * @return one row per distinct observable flow, in first-occurrence order
   */
  public static List<Map<String, Object>> rowsForMethod(
      final String artifact,
      final String methodSig,
      final List<ExceptionPath> paths,
      final Map<String, String> pathIdToStatus) {
    List<Map<String, Object>> rows = new ArrayList<>();
    if (paths == null || paths.isEmpty()) {
      return rows;
    }
    MethodParts parts = MethodParts.parseSignature(methodSig);
    Map<String, Map<String, Object>> rowByFlow = new LinkedHashMap<>();

    for (int i = 0; i < paths.size(); i++) {
      ExceptionPath ep = paths.get(i);
      List<Map<String, Object>> constraintRows = flattenConstraints(ep.getConstraints());
      String flowKey =
          ep.getExceptionQualifiedName()
              + "|"
              + ep.getThrowSiteKind().name()
              + "|"
              + constraintRows;

      Map<String, Object> existing = rowByFlow.get(flowKey);
      if (existing != null) {
        addProvenance(existing, ep.getProvenance());
        continue;
      }

      String pathId = methodSig + "#" + i;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("artifact", artifact);
      row.put("package", parts.pkg());
      row.put("class", parts.clazz());
      row.put("method", parts.method());
      row.put("returnType", parts.returnType());
      row.put("params", parts.params());
      row.put("pathIndex", i);
      row.put("pathId", pathId);
      row.put("exceptionType", ep.getExceptionQualifiedName());
      row.put("throwSiteKind", ep.getThrowSiteKind().name());
      row.put("provenance", ep.getProvenance());
      row.put("provenances", new ArrayList<List<String>>());
      // we should never see UNSOLVED; leaving here for debugging
      row.put("solverStatus", pathIdToStatus.getOrDefault(pathId, "UNSOLVED"));
      row.put("constraints", constraintRows);
      addProvenance(row, ep.getProvenance());
      rowByFlow.put(flowKey, row);
      rows.add(row);
    }
    return rows;
  }

  @SuppressWarnings("unchecked")
  private static void addProvenance(final Map<String, Object> row, final List<String> provenance) {
    List<List<String>> provenances = (List<List<String>>) row.get("provenances");
    if (provenance != null && !provenances.contains(provenance)) {
      provenances.add(provenance);
    }
  }

  // Each constraint becomes a `{symExpr, truthValue}` map. The list semantics is implicit
  // conjunction — every entry must hold for the path's exception to fire. Sufficient for
  // human eyeball-matching against Nassif's `State` column.
  static List<Map<String, Object>> flattenConstraints(final List<SymbolicConstraint> constraints) {
    if (constraints == null || constraints.isEmpty()) {
      return List.of();
    }
    List<Map<String, Object>> rows = new ArrayList<>(constraints.size());
    for (SymbolicConstraint c : constraints) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("symExpr", c.symExpr().toString());
      row.put("truthValue", c.truthValue());
      rows.add(row);
    }
    return rows;
  }
}
