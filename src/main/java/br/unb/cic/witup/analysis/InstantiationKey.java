package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.List;
import java.util.Objects;

public final class InstantiationKey {
  private final String calleeSig;
  private final List<String> actualKeys;

  InstantiationKey(final String calleeSig, final List<SymExpr> actuals) {
    this.calleeSig = calleeSig;
    this.actualKeys = actuals.stream().map(SymExpr::toString).toList();
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof InstantiationKey k)) {
      return false;
    }
    return calleeSig.equals(k.calleeSig) && actualKeys.equals(k.actualKeys);
  }

  @Override
  public int hashCode() {
    return Objects.hash(calleeSig, actualKeys);
  }
}
