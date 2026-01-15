package br.unb.cic.witup.analysis;

public class SymConst extends SymExpr {
    private final Object value; // Can be Integer, Double, String, etc.

    public SymConst(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public SymExpr substitute(String varName, SymExpr replacement) {
        // Constants don't contain variables
        return this;
    }

    @Override
    public boolean contains(String varName) {
        return false;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymConst)) return false;
        SymConst symConst = (SymConst) o;
        return value.equals(symConst.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
