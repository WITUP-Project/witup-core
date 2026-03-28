package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.List;

public record ResolvedCallee(
    List<GuardedExpr> guardedReturn, List<List<SymbolicConstraint>> throwPathConditions) {}
