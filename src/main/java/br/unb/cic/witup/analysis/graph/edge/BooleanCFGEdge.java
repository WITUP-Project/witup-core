package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.Objects;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/** A control flow graph edge with a boolean condition. */
public class BooleanCFGEdge extends CFGEdge {
  private final boolean condition;

  /**
   * Constructor for BooleanCFGEdge.
   *
   * @param edge the property graph edge
   * @param condition the boolean condition
   */
  public BooleanCFGEdge(
      final PropertyGraphEdge edge,
      final WITUpNode source,
      final WITUpNode target,
      final boolean condition) {
    super(edge, source, target);
    this.condition = condition;
  }

  /**
   * Gets the boolean condition.
   *
   * @return the condition
   */
  public boolean getCondition() {
    return condition;
  }

  @Override
  public final boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BooleanCFGEdge edge = (BooleanCFGEdge) o;
    return super.equals(edge) && condition == edge.condition;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(super.hashCode(), condition);
  }
}
