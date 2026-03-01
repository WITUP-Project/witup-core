package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.BinOp;

public final class SymBinOp extends SymExpr {
  private final BinOp op;
  private final SymExpr left;
  private final SymExpr right;

  public SymBinOp(final BinOp op, final SymExpr left, final SymExpr right) {
    this.op = op;
    this.left = left;
    this.right = right;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitBinOp(this);
  }

  public BinOp getOp() {
    return op;
  }

  public SymExpr getLeft() {
    return left;
  }

  public SymExpr getRight() {
    return right;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newLeft = left.substitute(varName, replacement);
    SymExpr newRight = right.substitute(varName, replacement);

    // when the SymExpr is SymVirtualInvoke that returns boolean, we need to do differently
    // $stack2 == 0 because the virtual invoke is s.isEmpty(), instead of is.Empty() == 0
    // I want isEmpty() true or false
    // newLeft would be isEmpty(), but what about the condition?
    if (newLeft != left || newRight != right) {
      return new SymBinOp(op, newLeft, newRight);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return left.contains(varName) || right.contains(varName);
  }

  @Override
  public String toString() {
    return "(" + left.toString() + " " + op.toString() + " " + right.toString() + ")";
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymBinOp)) {
      return false;
    }
    SymBinOp symBinOp = (SymBinOp) o;
    return op == symBinOp.op && left.equals(symBinOp.left) && right.equals(symBinOp.right);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * (prime * op.hashCode() + left.hashCode()) + right.hashCode();
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
