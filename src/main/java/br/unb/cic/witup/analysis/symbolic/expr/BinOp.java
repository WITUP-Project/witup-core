package br.unb.cic.witup.analysis.symbolic.expr;

public enum BinOp {
  EQ("=="),
  NE("!="),
  LT("<"),
  LE("<="),
  GT(">"),
  GE(">="),

  ADD("+"),
  SUB("-"),
  MUL("*"),
  DIV("/"),
  MOD("%"),

  // Special Jimple operations
  CMPG("cmpg"),
  CMPL("cmpl"),
  CMP("cmp"),

  SHIFT_RIGHT(">>"),
  UNSIGNED_SHIFT_RIGHT(">>>"),
  SHIFT_LEFT("<<"),

  AND("&&"),
  OR("||"),
  XOR("^");

  private final String symbol;

  BinOp(final String symbol) {
    this.symbol = symbol;
  }

  public String getSymbol() {
    return symbol;
  }

  @Override
  public String toString() {
    return symbol;
  }

  public static BinOp fromString(final String op) {
    for (BinOp binOp : values()) {
      if (binOp.symbol.equals(op)) {
        return binOp;
      }
    }
    throw new IllegalArgumentException("Unknown operation: " + op);
  }
}
