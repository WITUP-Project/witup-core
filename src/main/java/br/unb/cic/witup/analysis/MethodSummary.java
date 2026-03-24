package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.List;

public final class MethodSummary {
  private final String methodSignature;
  private final List<List<SymbolicConstraint>> symbolicConstraintPaths;
  private final List<SymParamRef> formalParams;
  private final SymExpr returnExpr;
  private final SymExpr throwFreePrecondition;

  public MethodSummary(
      final String methodSignature,
      final List<List<SymbolicConstraint>> symbolicConstraintPaths,
      final List<SymParamRef> formalParams,
      final SymExpr returnExpr,
      final SymExpr throwFreePrecondition) {
    this.methodSignature = methodSignature;
    this.symbolicConstraintPaths = symbolicConstraintPaths;
    this.formalParams = formalParams;
    this.returnExpr = returnExpr;
    this.throwFreePrecondition = throwFreePrecondition;
  }

  public SymExpr getThrowFreePrecondition() {
    return throwFreePrecondition;
  }

  public boolean hasThrowFreePrecondition() {
    return throwFreePrecondition != null;
  }

  public static MethodSummary empty(final String sig) {
    return new MethodSummary(sig, null, null, null, null);
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
