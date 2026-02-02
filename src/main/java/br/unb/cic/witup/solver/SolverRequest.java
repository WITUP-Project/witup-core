package br.unb.cic.witup.solver;

import java.util.List;

public record SolverRequest(List<Path> paths) {

  public record Path(String pathId, List<Condition> conditions) {}

  public record Condition(boolean truthValue, String conditionExpr) {}
}
