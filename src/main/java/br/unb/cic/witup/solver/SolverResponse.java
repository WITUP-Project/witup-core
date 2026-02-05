package br.unb.cic.witup.solver;

import java.util.List;

public final class SolverResponse {

  public static final class PathResult {
    private final String pathId;
    private final boolean isSat;
    private final List<Solution> solutions;

    public PathResult(final String pathId, final boolean isSat, final List<Solution> solutions) {
      this.pathId = pathId;
      this.isSat = isSat;
      this.solutions = solutions;
    }

    public String getPathId() {
      return pathId;
    }

    public boolean isSat() {
      return isSat;
    }

    public List<Solution> getSolutions() {
      return solutions;
    }
  }

  public static final class Solution {
    private final String variable;
    private final String value;

    public Solution(final String variable, final String value) {
      this.variable = variable;
      this.value = value;
    }

    public String getVariable() {
      return variable;
    }

    public String getValue() {
      return value;
    }
  }

  private final List<PathResult> results;

  public SolverResponse(final List<PathResult> results) {
    this.results = results;
  }

  public List<PathResult> getResults() {
    return results;
  }
}
