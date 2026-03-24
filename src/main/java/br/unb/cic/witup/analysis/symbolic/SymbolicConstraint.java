package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;

public record SymbolicConstraint(SymExpr symExpr, boolean truthValue) {

}
