package br.unb.cic.witup.solver;

import com.microsoft.z3.Status;

import java.util.Map;

public record SolverResult(
        String pathId,
        Status status,
        Map<String, String> model
) {
  public boolean isSat() {
    return status == Status.SATISFIABLE;
  }
  public boolean isUnsat() {
    return status == Status.UNSATISFIABLE;
  }
}