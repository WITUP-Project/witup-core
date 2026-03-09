package br.unb.cic.witup.analysis.summary;

import br.unb.cic.witup.analysis.MethodAnalysis;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.List;

public final class SummaryGenerator {
  private final SummaryCache cache;

  public SummaryGenerator(final SummaryCache cache) {
    this.cache = cache;
  }

  public MethodSummary summarise(final MethodAnalysis analysis) {
    String sig = analysis.getMethodSignature();

    if (cache.get(sig).isPresent()) {
      return cache.get(sig).get();
    }
    if (cache.isInProgress(sig)) {
      return conservativeSummary(sig);
    }

    cache.markInProgress(sig);

    List<ThrowCase> throwCases = new ArrayList<>();
    for (WITUpNode throwNode : analysis.getThrowNodes()) {
      for (List<SymbolicConstraint> path : analysis.getSymbolicConstraintPaths(throwNode)) {
        for (SymbolicConstraint sc : path) {
          throwCases.add(new ThrowCase(sc.getSymExpr(), null, sc.getTruthValue()));
        }
      }
    }

    MethodSummary summary = new MethodSummary(sig, throwCases);
    cache.put(sig, summary);
    return summary;
  }

  private MethodSummary conservativeSummary(final String sig) {
    return new MethodSummary(sig, List.of(new ThrowCase(SymConst.TRUE, null, true)));
  }
}
