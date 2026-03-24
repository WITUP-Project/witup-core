package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;

public final class SymITE extends SymExpr {
  private final SymExpr condition;
  private final SymExpr thenExpr;
  private final SymExpr elseExpr;
  private String cachedToString;

  public SymITE(final SymExpr condition, final SymExpr thenExpr, final SymExpr elseExpr) {
    super(thenExpr.getKind());
    this.condition = condition;
    this.thenExpr = thenExpr;
    this.elseExpr = elseExpr;
  }

  public SymExpr getCondition() {
    return condition;
  }

  public SymExpr getThenExpr() {
    return thenExpr;
  }

  public SymExpr getElseExpr() {
    return elseExpr;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitITE(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newCond = condition.substitute(varName, replacement);
    SymExpr newThen = thenExpr.substitute(varName, replacement);
    SymExpr newElse = elseExpr.substitute(varName, replacement);
    if (newCond != condition || newThen != thenExpr || newElse != elseExpr) {
      return new SymITE(newCond, newThen, newElse);
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    if (!containsParam(idx)) {
      return this;
    }
    SymExpr newCond = condition.substituteParam(idx, actual);
    SymExpr newThen = thenExpr.substituteParam(idx, actual);
    SymExpr newElse = elseExpr.substituteParam(idx, actual);
    if (newCond != condition || newThen != thenExpr || newElse != elseExpr) {
      return new SymITE(newCond, newThen, newElse);
    }
    return this;
  }

  @Override
  public boolean containsUnboxing() {
    return condition.containsUnboxing()
            || thenExpr.containsUnboxing()
            || elseExpr.containsUnboxing();
  }

  @Override
  public boolean containsParam(final int idx) {
    return condition.containsParam(idx)
            || thenExpr.containsParam(idx)
            || elseExpr.containsParam(idx);
  }

  @Override
  public boolean contains(final String varName) {
    return condition.contains(varName) || thenExpr.contains(varName) || elseExpr.contains(varName);
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = "(" + condition + " ? " + thenExpr + " : " + elseExpr + ")";
    }
    return cachedToString;
  }
}
