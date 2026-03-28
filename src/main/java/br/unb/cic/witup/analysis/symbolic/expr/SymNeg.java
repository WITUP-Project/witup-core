package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Map;
import java.util.Set;
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
  public void collectVarNames(final Set<String> vars) {
    operand.collectVarNames(vars);
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    SymExpr newOperand = operand.resolveWith(env);
    return (newOperand != operand) ? new SymNeg(newOperand, getKind()) : this;
  }

  @Override
  public String toString() {
    return cachedToString;
  }
}
