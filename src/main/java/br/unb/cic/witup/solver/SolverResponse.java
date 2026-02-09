package br.unb.cic.witup.solver;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SolverResponse {
  private final List<SolverPathResult> paths;

  public enum Status {
    SAT, UNSAT, UNKNOWN, ERROR
  }

  @JsonCreator
  public SolverResponse(@JsonProperty("paths") List<SolverPathResult> paths) {
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
            @JsonProperty("pathId") String pathId,
            @JsonProperty("status") Status status,
            @JsonProperty("solutions") List<SolverPathSolution> solverPathSolutions) {
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
            @JsonProperty("symbol") String symbol,
            @JsonProperty("value") String value) {
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
