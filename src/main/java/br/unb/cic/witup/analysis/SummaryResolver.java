package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.List;
import java.util.Optional;

public interface SummaryResolver {
  Optional<ResolvedCallee> resolveCallee(String calleeSignature, List<SymExpr> actuals);
}
