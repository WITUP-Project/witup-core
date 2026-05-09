package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.List;

public final class ExceptionPath {
  private final List<SymbolicConstraint> constraints;
  private final WITUpNode throwNode;
  private final String exceptionQualifiedName;
  private final ThrowSiteKind throwSiteKind;
  private final List<String> provenance;

  public ExceptionPath(
      final List<SymbolicConstraint> constraints,
      final WITUpNode throwNode,
      final String exceptionQualifiedName,
      final ThrowSiteKind throwSiteKind,
      final List<String> provenance) {
    this.constraints = constraints;
    this.throwNode = throwNode;
    this.exceptionQualifiedName = exceptionQualifiedName;
    this.throwSiteKind = throwSiteKind;
    this.provenance = provenance;
  }

  public List<SymbolicConstraint> getConstraints() {
    return constraints;
  }

  public WITUpNode getThrowNode() {
    return throwNode;
  }

  public String getExceptionQualifiedName() {
    return exceptionQualifiedName;
  }

  public ThrowSiteKind getThrowSiteKind() {
    return throwSiteKind;
  }

  public List<String> getProvenance() {
    return provenance;
  }
}
