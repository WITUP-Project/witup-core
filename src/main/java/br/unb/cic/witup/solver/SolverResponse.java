package br.unb.cic.witup.solver;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class SolverResponse {
  private final List<SolverPathResult> paths;

  public enum Status {
    SAT,
    UNSAT,
    UNKNOWN,
    ERROR
  }

  @JsonCreator
  public SolverResponse(@JsonProperty("paths") final List<SolverPathResult> paths) {
    this.paths = paths;
  }

  public List<SolverPathResult> getPaths() {
    return paths;
  }

  public static final class SolverPathResult {
    private final String pathId;
    private final Status status;
    private final List<SolverPathSolution> solverPathSolutions;

    @JsonCreator
    public SolverPathResult(
        @JsonProperty("pathId") final String pathId,
        @JsonProperty("status") final Status status,
        @JsonProperty("solutions") final List<SolverPathSolution> solverPathSolutions) {
      this.pathId = pathId;
      this.status = status;
      this.solverPathSolutions = solverPathSolutions;
    }

    public String getPathId() {
      return pathId;
    }

    public Status getStatus() {
      return status;
    }

    public boolean isSat() {
      return status == Status.SAT;
    }

    public List<SolverPathSolution> getSolutions() {
      return solverPathSolutions;
    }
  }

  public static final class SolverPathSolution {
    private final String symbol;
    private final String value;

    @JsonCreator
    public SolverPathSolution(
        @JsonProperty("symbol") final String symbol, @JsonProperty("value") final String value) {
      this.symbol = symbol;
      this.value = value;
    }

    public String getSymbol() {
      return symbol;
    }

    public String getValue() {
      return value;
    }
  }
}
