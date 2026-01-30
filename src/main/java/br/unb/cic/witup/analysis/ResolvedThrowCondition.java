package br.unb.cic.witup.analysis;

public final class ResolvedThrowCondition {
  private SymExpr node;
  private boolean truthValue;

  public ResolvedThrowCondition(final SymExpr node, final boolean truthValue) {
    this.node = node;
    this.truthValue = truthValue;
  }

  public SymExpr getNode() {
    return node;
  }

  public boolean isTruthValue() {
    return truthValue;
  }
}
