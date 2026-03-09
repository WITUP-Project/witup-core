package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.ArrayList;
import java.util.List;

public final class SummaryGenerator {
  public SummaryGenerator() {};

  public MethodSummary summarise(final MethodAnalysis analysis) {
    String sig = analysis.getMethodSignature();

    List<ThrowCase> throwCases = new ArrayList<>();
    for (WITUpNode throwNode : analysis.getThrowNodes()) {
      for (List<SymbolicConstraint> path : analysis.getSymbolicConstraintPaths(throwNode)) {
        for (SymbolicConstraint sc : path) {
          throwCases.add(new ThrowCase(sc.getSymExpr(), null, sc.getTruthValue()));
        }
      }
    }

    return new MethodSummary(sig, throwCases);
  }

  private MethodSummary conservativeSummary(final String sig) {
    return new MethodSummary(sig, List.of(new ThrowCase(SymConst.TRUE, null, true)));
  }
}
