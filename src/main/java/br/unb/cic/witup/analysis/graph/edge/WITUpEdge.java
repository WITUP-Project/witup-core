package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.Objects;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;

/** Abstract base class for WITUpGraph edges. Wraps SootUp PropertyGraphEdge type */
public abstract class WITUpEdge {
  private final PropertyGraphEdge edge;
  private final WITUpNode source;
  private final WITUpNode target;

  /**
   * Constructor for WITUpEdge.
   *
   * @param edge the property graph edge
   */
  public WITUpEdge(final PropertyGraphEdge edge, final WITUpNode source, final WITUpNode target) {
    this.edge = edge;
    this.source = source;
    this.target = target;
  }

  public final WITUpNode getSource() {
    return source;
  }

  public final WITUpNode getTarget() {
    return target;
  }

  /**
   * Gets the underlying property graph edge.
   *
   * @return the property graph edge
   */
  public PropertyGraphEdge getEdge() {
    return edge;
  }

  /**
   * Extending classes must use respective type casts
   *
   * @param o the reference object with which to compare.
   * @return boolean result of comparison
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WITUpEdge otherEdge = (WITUpEdge) o;
    return Objects.equals(this.edge, otherEdge.edge);
  }

  /**
   * Overriding methods must hash the edge alongside other paramenters
   *
   * @return int a hash of the edge
   */
  @Override
  public int hashCode() {
    return Objects.hash(edge);
  }
}
