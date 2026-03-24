package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.AbstractBinopExpr;

public final class SymBinOp extends SymExpr {
  private final BinOp op;
  private final SymExpr lhs;
  private final SymExpr rhs;
  private String cachedToString;

  public static SymExpr fromBinopExpr(final AbstractBinopExpr e) {
    SymExpr lhs = fromJimple(e.getOp1());
    SymExpr rhs = fromJimple(e.getOp2());
    return new SymBinOp(fromJimpleBinop(e), lhs, rhs);
  }

  public SymBinOp(final BinOp op, final SymExpr lhs, final SymExpr rhs) {
    super(deriveKind(lhs, rhs));
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  private static SymKind deriveKind(final SymExpr left, final SymExpr right) {
    if (left.getKind() == SymKind.BOOLEAN_METHOD || left.getKind() == SymKind.BOOLEAN) {
      return left.getKind();
    }
    if (right.getKind() == SymKind.BOOLEAN_METHOD || right.getKind() == SymKind.BOOLEAN) {
      return right.getKind();
    }
    return SymKind.OTHER;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitBinOp(this);
  }

  public BinOp getOp() {
    return op;
  }

  public SymExpr getLhs() {
    return lhs;
  }

  public SymExpr getRhs() {
    return rhs;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    if (!containsParam(idx)) {
      return this;
    }
    SymExpr newLhs = lhs.substituteParam(idx, actual);
    SymExpr newRhs = rhs.substituteParam(idx, actual);
    if (newLhs != lhs || newRhs != rhs) {
      return new SymBinOp(op, newLhs, newRhs);
    }
    return this;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newLhs = lhs.substitute(varName, replacement);
    SymExpr newRhs = rhs.substitute(varName, replacement);

    // when the SymExpr is SymVirtualInvoke that returns boolean, we need to do differently
    // $stack2 == 0 because the virtual invoke is s.isEmpty(), instead of is.Empty() == 0
    // I want isEmpty() true or false
    // newLeft would be isEmpty(), but what about the condition?
    if (newLhs != lhs || newRhs != rhs) {
      return new SymBinOp(op, newLhs, newRhs);
    }
    return this;
  }

  @Override
  public boolean containsUnboxing() {
    return lhs.containsUnboxing() || rhs.containsUnboxing();
  }

  @Override
  public boolean containsParam(final int idx) {
    return lhs.containsParam(idx) || rhs.containsParam(idx);
  }

  @Override
  public boolean contains(final String varName) {
    return lhs.contains(varName) || rhs.contains(varName);
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      cachedToString = "(" + lhs.toString() + " " + op.toString() + " " + rhs.toString() + ")";
    }
    return cachedToString;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymBinOp symBinOp)) {
      return false;
    }
    return op == symBinOp.op && lhs.equals(symBinOp.lhs) && rhs.equals(symBinOp.rhs);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * (prime * op.hashCode() + lhs.hashCode()) + rhs.hashCode();
  }
}
