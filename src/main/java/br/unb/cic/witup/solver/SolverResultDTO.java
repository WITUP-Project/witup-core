package br.unb.cic.witup.solver;

import java.util.Map;
import java.util.stream.Collectors;

public record SolverResultDTO(String pathId, String status, Map<String, String> modelValues) {
  public static SolverResultDTO from(final SolverResult r) {
    Map<String, String> values =
        r.getModelValueMap().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    return new SolverResultDTO(r.getPathId(), r.getStatus().toString(), values);
  }
}
