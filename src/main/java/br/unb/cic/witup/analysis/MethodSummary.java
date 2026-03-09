package br.unb.cic.witup.analysis.summary;

import java.util.List;

public final class MethodSummary {
  private final String methodSignature;
  private final List<ThrowCase> throwCases;

  public MethodSummary(final String methodSignature, final List<ThrowCase> throwCases) {
    this.methodSignature = methodSignature;
    this.throwCases = List.copyOf(throwCases);
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public List<ThrowCase> getThrowCases() {
    return throwCases;
  }
}
