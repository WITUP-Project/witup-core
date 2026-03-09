package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;

import java.util.List;

public final class MethodSummary {
  private final String methodSignature;
  private final List<SymbolicConstraint> symbolicConstraints;

  public MethodSummary(
      final String methodSignature, final List<SymbolicConstraint> symbolicConstraints) {
    this.methodSignature = methodSignature;
    this.symbolicConstraints = List.copyOf(symbolicConstraints);
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public List<SymbolicConstraint> getSymbolicConstraints() {
    return symbolicConstraints;
  }
}
