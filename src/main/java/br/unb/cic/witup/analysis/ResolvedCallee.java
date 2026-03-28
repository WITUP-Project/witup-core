package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.GuardedExpr;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.List;

public record ResolvedCallee(
    SymExpr returnExpr,
    SymExpr precondition,
    List<GuardedExpr> guardedReturn,
    List<List<SymbolicConstraint>> throwPathConditions) {}
