package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.ArrayList;
import java.util.List;

/**
 * A guarded expression: value is meaningful when guard conditions hold. Bindings carry
 * unconditional constraints that define fresh variables referenced in value or guard (from
 * interprocedural resolution of callees).
 */
public record GuardedExpr(
    List<SymbolicConstraint> guard, SymExpr value, List<SymbolicConstraint> bindings) {

  public GuardedExpr(final List<SymbolicConstraint> guard, final SymExpr value) {
    this(guard, value, List.of());
  }

  public GuardedExpr substituteParam(final int idx, final SymExpr actual) {
    List<SymbolicConstraint> newGuard = new ArrayList<>(guard.size());
    for (SymbolicConstraint c : guard) {
      newGuard.add(
          new SymbolicConstraint(c.symExpr().substituteParam(idx, actual), c.truthValue()));
    }
    List<SymbolicConstraint> newBindings = new ArrayList<>(bindings.size());
    for (SymbolicConstraint c : bindings) {
      newBindings.add(
          new SymbolicConstraint(c.symExpr().substituteParam(idx, actual), c.truthValue()));
    }
    SymExpr newValue = value.substituteParam(idx, actual);
    return new GuardedExpr(newGuard, newValue, newBindings);
  }
}
