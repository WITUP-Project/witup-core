package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.common.expr.JLengthExpr;

public final class SymLength extends SymExpr {
  private final SymExpr op;
  private String cachedToString;

  public SymLength(final JLengthExpr e) {
    this(fromJimple(e.getOp()));
  }

  public SymLength(final SymExpr op) {
    super(SymKind.INT, maskOf(op));
    this.op = op;
  }

  public SymExpr getOp() {
    return op;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitLength(this);
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newOp = op.substituteParam(idx, actual);
    return newOp == op ? this : new SymLength(newOp);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newOp = op.substitute(varName, replacement);
    if (newOp != op) {
      return new SymLength(newOp);
    }
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = op.toString() + ".length";
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return op.contains(varName);
  }

  @Override
  public void collectVarNames(final Set<String> vars) {
    op.collectVarNames(vars);
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    SymExpr newOp = op.resolveWith(env);
    return (newOp != op) ? new SymLength(newOp) : this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymLength symLength)) {
      return false;
    }
    return op.equals(symLength.op);
  }

  @Override
  public int hashCode() {
    return op.hashCode();
  }
}
