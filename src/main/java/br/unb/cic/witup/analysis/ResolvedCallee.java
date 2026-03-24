package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;

public record ResolvedCallee(SymExpr returnExpr, SymExpr precondition) {}
