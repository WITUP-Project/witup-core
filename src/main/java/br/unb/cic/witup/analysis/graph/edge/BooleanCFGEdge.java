package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;

/** A control flow graph edge with a boolean condition. */
public final class BooleanCFGEdge extends CFGEdge {
  public static final int THIRTY_ONE = 31;
  private final boolean condition;

  public BooleanCFGEdge(
      final WITUpNode source,
      final WITUpNode target,
      final boolean condition) {
    super(source, target);
    this.condition = condition;
  }

  public boolean getCondition() {
    return condition;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BooleanCFGEdge that = (BooleanCFGEdge) o;
    return super.equals(that) && condition == that.condition;
  }

  @Override
  public int hashCode() {
    int h = super.hashCode();
    return THIRTY_ONE * h + (condition ? 1 : 0);
  }
}
