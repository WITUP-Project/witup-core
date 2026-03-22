package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JNegExpr;

public final class SymNeg extends SymExpr {
  private final SymExpr operand;
  private String cachedToString;

  public SymNeg(final JNegExpr e) {
    super(fromJimpleType(e.getType()));
    this.operand = fromJimple(e.getOp());
  }

  private SymNeg(final SymExpr operand, final SymKind kind) {
    super(kind);
    this.operand = operand;
    this.cachedToString = "-" + operand;
  }

  public SymExpr getOperand() {
    return operand;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitNeg(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newOperand = operand.substitute(varName, replacement);
    if (newOperand != operand) {
      return new SymNeg(newOperand, getKind());
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newOperand = operand.substituteParam(idx, actual);
    if (newOperand != operand) {
      return new SymNeg(newOperand, getKind());
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return operand.contains(varName);
  }

  @Override
  public String toString() {
    return cachedToString;
  }
}
