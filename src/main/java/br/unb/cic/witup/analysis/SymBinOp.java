package br.unb.cic.witup.analysis;

public final class SymBinOp extends SymExpr {
  private final BinOp op;
  private final SymExpr left;
  private final SymExpr right;

  public SymBinOp(final BinOp op, final SymExpr left, final SymExpr right) {
    this.op = op;
    this.left = left;
    this.right = right;
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
}
