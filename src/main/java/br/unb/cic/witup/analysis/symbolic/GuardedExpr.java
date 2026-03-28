package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.ArrayList;
import java.util.List;

public record GuardedExpr(List<SymbolicConstraint> guard, SymExpr value) {

  public GuardedExpr substituteParam(final int idx, final SymExpr actual) {
    List<SymbolicConstraint> newGuard = new ArrayList<>(guard.size());
    for (SymbolicConstraint c : guard) {
      newGuard.add(
          new SymbolicConstraint(c.symExpr().substituteParam(idx, actual), c.truthValue()));
    }
    SymExpr newValue = value.substituteParam(idx, actual);
    return new GuardedExpr(newGuard, newValue);
  }
}
