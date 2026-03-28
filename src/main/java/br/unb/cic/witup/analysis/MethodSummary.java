package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.List;

public final class MethodSummary {
  private final String methodSignature;
  private final List<ExceptionPath> exceptionPaths;
  private final List<SymParamRef> formalParams;
  private final SymExpr returnExpr;
  private final SymExpr throwFreePrecondition;
  private final List<GuardedExpr> guardedReturn;
  private final List<List<SymbolicConstraint>> throwPathConditions;

  public MethodSummary(
      final String methodSignature,
      final List<ExceptionPath> exceptionPaths,
      final List<SymParamRef> formalParams,
      final SymExpr returnExpr,
      final SymExpr throwFreePrecondition) {
    this(
        methodSignature,
        exceptionPaths,
        formalParams,
        returnExpr,
        throwFreePrecondition,
        null,
        null);
  }

  public MethodSummary(
      final String methodSignature,
      final List<ExceptionPath> exceptionPaths,
      final List<SymParamRef> formalParams,
      final SymExpr returnExpr,
      final SymExpr throwFreePrecondition,
      final List<GuardedExpr> guardedReturn,
      final List<List<SymbolicConstraint>> throwPathConditions) {
    this.methodSignature = methodSignature;
    this.exceptionPaths = exceptionPaths;
    this.formalParams = formalParams;
    this.returnExpr = returnExpr;
    this.throwFreePrecondition = throwFreePrecondition;
    this.guardedReturn = guardedReturn;
    this.throwPathConditions = throwPathConditions;
  }

  public SymExpr getThrowFreePrecondition() {
    return throwFreePrecondition;
  }

  public List<ExceptionPath> getExceptionPaths() {
    return exceptionPaths;
  }

  public static MethodSummary empty(final String sig) {
    return new MethodSummary(sig, null, null, null, null);
  }

  public String getMethodSignature() {
    return methodSignature;
  }

  public List<SymParamRef> getFormalParams() {
    return formalParams;
  }

  public SymExpr getReturnExpr() {
    return returnExpr;
  }

  public boolean hasReturnExpr() {
    return returnExpr != null || guardedReturn != null;
  }

  public List<GuardedExpr> getGuardedReturn() {
    return guardedReturn;
  }

  public List<List<SymbolicConstraint>> getThrowPathConditions() {
    return throwPathConditions;
  }
}
