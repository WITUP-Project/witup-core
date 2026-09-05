package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Set;
import sootup.core.jimple.common.expr.JInstanceOfExpr;

public final class SymInstanceOf extends SymExpr {
  private final SymExpr op;
  private final String type;
  private String cachedToString;
  private int cachedHashCode;

  public SymInstanceOf(final JInstanceOfExpr e) {
    this(fromJimple(e.getOp()), e.getCheckType().toString());
  }

  private SymInstanceOf(final SymExpr op, final String type) {
    super(SymKind.BOOLEAN, maskOf(op));
    this.op = op;
    this.type = type;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newOp = op.substituteParam(idx, actual);
    return newOp == op ? this : new SymInstanceOf(newOp, type);
  }

  public SymExpr getOp() {
    return op;
  }

  public String getType() {
    return type;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitInstanceOf(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = op.toString() + "_instanceof_" + type.replace(".", "_");
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
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymInstanceOf symInstanceOf)) {
      return false;
    }
    return op.equals(symInstanceOf.op) && type.equals(symInstanceOf.type);
  }

  @Override
  public int hashCode() {
    int h = cachedHashCode;
    if (h == 0) {
      final int prime = 31;
      h = prime * op.hashCode() + type.hashCode();
      cachedHashCode = h;
    }
    return h;
  }
}
