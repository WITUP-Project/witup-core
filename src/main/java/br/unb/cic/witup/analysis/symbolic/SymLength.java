package br.unb.cic.witup.analysis.symbolic;

public class SymLength extends SymExpr {
  SymExpr op;

  public SymLength(final SymExpr op) {
    this.op = op;
  }

  public SymExpr getOp() {
    return op;
  }

  @Override
  public <T> T accept(SymExprVisitor<T> visitor) {
    return visitor.visitLength(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    SymExpr newOp =  op.substitute(varName, replacement);
    if (newOp != op) {
      return new SymLength(newOp);
    }
    return this;
  }

  @Override
  public String toString() {
    return op.toString() + ".length";
  }

  @Override
  public boolean contains(String varName) {
    return op.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.OTHER;
  }
}
