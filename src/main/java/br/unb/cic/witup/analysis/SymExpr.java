package br.unb.cic.witup.analysis;

public abstract class SymExpr {
    /**
     * Substitute a variable with another expression
     */
    public abstract SymExpr substitute(String varName, SymExpr replacement);

    /**
     * Pretty print for debugging
     */
    public abstract String toString();

    /**
     * Check if this expression contains a given variable
     */
    public abstract boolean contains(String varName);
}
