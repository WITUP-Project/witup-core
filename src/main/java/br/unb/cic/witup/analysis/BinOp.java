package br.unb.cic.witup.analysis;

public enum BinOp {
    // Comparison
    EQ("=="),
    NE("!="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),

    // Arithmetic
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    MOD("%"),

    // Special Jimple operations
    CMPG("cmpg"),
    CMPL("cmpl"),
    CMP("cmp");

    private final String symbol;

    BinOp(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }

    public static BinOp fromString(String op) {
        for (BinOp binOp : values()) {
            if (binOp.symbol.equals(op)) {
                return binOp;
            }
        }
        throw new IllegalArgumentException("Unknown operation: " + op);
    }
}