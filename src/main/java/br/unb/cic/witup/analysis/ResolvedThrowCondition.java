package br.unb.cic.witup.analysis;

public class ResolvedThrowCondition {
    SymExpr node;
    boolean truthValue;

    public ResolvedThrowCondition(SymExpr node, boolean truthValue) {
        this.node = node;
        this.truthValue = truthValue;
    }
}
