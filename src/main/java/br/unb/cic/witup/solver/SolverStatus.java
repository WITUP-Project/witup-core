package br.unb.cic.witup.solver;

import com.microsoft.z3.Status;

public enum SolverStatus {
  SAT,
  UNSAT,
  UNKNOWN,
  MAYBE;

  public static SolverStatus fromZ3(final Status z3Status) {
    return switch (z3Status) {
      case SATISFIABLE -> SAT;
      case UNSATISFIABLE -> UNSAT;
      case UNKNOWN -> UNKNOWN;
    };
  }
}
