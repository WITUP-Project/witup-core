package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.List;

public final class ExceptionPath {
  private final List<SymbolicConstraint> constraints;
  private final WITUpNode throwNode;
  private final String exceptionType;

  public ExceptionPath(
      final List<SymbolicConstraint> constraints,
      final WITUpNode throwNode,
      final String exceptionType) {
    this.constraints = constraints;
    this.throwNode = throwNode;
    this.exceptionType = exceptionType;
  }

  public List<SymbolicConstraint> getConstraints() {
    return constraints;
  }

  public WITUpNode getThrowNode() {
    return throwNode;
  }

  public String getExceptionType() {
    return exceptionType;
  }
}
