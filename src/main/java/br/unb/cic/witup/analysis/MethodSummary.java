package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.List;

public final class MethodSummary {
  private final String methodSignature;
  private final List<List<SymbolicConstraint>> symbolicConstraintPaths;

  public MethodSummary(
      final String methodSignature, final List<List<SymbolicConstraint>> symbolicConstraints) {
    this.methodSignature = methodSignature;
    this.symbolicConstraintPaths = symbolicConstraints;
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public List<List<SymbolicConstraint>> getSymbolicConstraintPaths() {
    return symbolicConstraintPaths;
  }
}
