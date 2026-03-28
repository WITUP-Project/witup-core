package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.List;

public record MethodSummary(
    String methodSignature,
    List<ExceptionPath> exceptionPaths,
    List<SymParamRef> formalParams,
    List<GuardedExpr> guardedReturn,
    List<List<SymbolicConstraint>> throwConstraints) {

  public static MethodSummary empty(final String sig) {
    return new MethodSummary(sig, null, null, null, null);
  }

  public boolean hasReturnExpr() {
    return guardedReturn != null && !guardedReturn.isEmpty();
  }
}
