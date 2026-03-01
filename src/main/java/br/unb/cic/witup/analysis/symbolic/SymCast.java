package br.unb.cic.witup.analysis.symbolic;

public class SymCast extends SymExpr {
  SymExpr op;
  String type;

  public SymCast(SymExpr op, String type) {
    this.op = op;
    this.type = type;
  }

  public SymExpr getOp() {
    return op;
  }

  public String getType() {
    return type;
  }

  @Override
  public <T> T accept(SymExprVisitor<T> visitor) {
    return visitor.visitCast(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return "(" + type + ")" + op.toString();
  }

  @Override
  public boolean contains(String varName) {
    return op.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.CAST;
  }
}
