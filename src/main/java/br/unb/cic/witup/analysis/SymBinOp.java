package br.unb.cic.witup.analysis;

public class SymBinOp extends SymExpr {
    private final BinOp op;
    private final SymExpr left;
    private final SymExpr right;

    public SymBinOp(BinOp op, SymExpr left, SymExpr right) {
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
    public SymExpr substitute(String varName, SymExpr replacement) {
        SymExpr newLeft = left.substitute(varName, replacement);
        SymExpr newRight = right.substitute(varName, replacement);

        if (newLeft != left || newRight != right) {
            return new SymBinOp(op, newLeft, newRight);
        }
        return this;
    }

    @Override
    public boolean contains(String varName) {
        return left.contains(varName) || right.contains(varName);
    }

    @Override
    public String toString() {
        return "(" + left.toString() + " " + op.toString() + " " + right.toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymBinOp)) return false;
        SymBinOp symBinOp = (SymBinOp) o;
        return op == symBinOp.op &&
                left.equals(symBinOp.left) &&
                right.equals(symBinOp.right);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * op.hashCode() + left.hashCode()) + right.hashCode();
    }
}
