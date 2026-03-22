package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymExpr;
import java.util.List;
import java.util.Optional;

public interface SummaryResolver {
  Optional<MethodSummariser.ResolvedCallee> resolveReturnExpr(
      String calleeSignature, List<SymExpr> actuals);
}
