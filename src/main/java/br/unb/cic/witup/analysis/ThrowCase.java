package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.SymExpr;
import br.unb.cic.witup.analysis.symbolic.SymParam;
import java.util.List;
import sootup.core.types.ClassType;

public record ThrowCase(SymExpr condition, ClassType exceptionType, boolean truthValue) {
  public ThrowCase instantiate(final List<SymParam> formals, final List<SymExpr> actuals) {
    SymExpr instantiated = condition;
    for (int i = 0; i < formals.size(); i++) {
      instantiated = instantiated.substituteParam(formals.get(i).getIndex(), actuals.get(i));
    }
    return new ThrowCase(instantiated, exceptionType, truthValue);
  }
}
