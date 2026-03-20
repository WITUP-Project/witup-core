package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymExpr;
import br.unb.cic.witup.analysis.symbolic.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.List;

public final class MethodSummary {
  private final String methodSignature;
  private final List<List<SymbolicConstraint>> symbolicConstraintPaths;
  private final List<SymParamRef> formalParams;
  private final SymExpr returnExpr;

  public MethodSummary(
      final String methodSignature, final List<List<SymbolicConstraint>> symbolicConstraintPaths) {
    this(methodSignature, symbolicConstraintPaths, null, null);
  }

  public MethodSummary(
      final String methodSignature,
      final List<List<SymbolicConstraint>> symbolicConstraintPaths,
      final List<SymParamRef> formalParams,
      final SymExpr returnExpr) {
    this.methodSignature = methodSignature;
    this.symbolicConstraintPaths = symbolicConstraintPaths;
    this.formalParams = formalParams;
    this.returnExpr = returnExpr;
  }

  public static MethodSummary empty(String sig) {
    return new MethodSummary(sig, null, null, null);
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public List<List<SymbolicConstraint>> getSymbolicConstraintPaths() {
    return symbolicConstraintPaths;
  }

  public List<SymParamRef> getFormalParams() {
    return formalParams;
  }

  public SymExpr getReturnExpr() {
    return returnExpr;
  }

  public boolean hasReturnExpr() {
    return returnExpr != null;
  }
}
