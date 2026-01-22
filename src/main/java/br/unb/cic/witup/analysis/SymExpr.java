package br.unb.cic.witup.analysis;

public abstract class SymExpr {
    /**
     * Substitute a variable with another expression
     */
    public abstract SymExpr substitute(String varName, SymExpr replacement);

    public abstract String toString();

    public abstract boolean contains(String varName);
}
