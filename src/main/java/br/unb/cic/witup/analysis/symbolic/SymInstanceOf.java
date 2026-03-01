package br.unb.cic.witup.analysis.symbolic;

public class SymInstanceOf extends SymExpr {
  SymExpr op;
  String type;

  public SymInstanceOf(SymExpr op, String type) {
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
    return visitor.visitInstanceOf(this);
  }

  @Override
  public SymExpr substitute(String varName, SymExpr replacement) {
    return this;
  }

  @Override
  public String toString() {
    return op.toString() + "_instanceof_" + type.replace(".", "_");
  }

  @Override
  public boolean contains(String varName) {
    return op.contains(varName);
  }

  @Override
  public SymKind kind() {
    return SymKind.BOOLEAN;
  }
}
