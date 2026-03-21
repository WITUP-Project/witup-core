package br.unb.cic.witup.analysis.graph.edge;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;

/** Abstract base class for WITUpGraph edges. Wraps SootUp PropertyGraphEdge type */
public abstract class WITUpEdge {
  public static final int THIRTY_ONE = 31;
  private final WITUpNode source;
  private final WITUpNode target;

  public WITUpEdge(final WITUpNode source, final WITUpNode target) {
    this.source = source;
    this.target = target;
  }

  public final WITUpNode getSource() {
    return source;
  }

  public final WITUpNode getTarget() {
    return target;
  }

  /** compares witup nodes source and target */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WITUpEdge that = (WITUpEdge) o;
    return source == that.source && target == that.target;
  }

  /** */
  @Override
  public int hashCode() {
    return System.identityHashCode(source) * THIRTY_ONE + System.identityHashCode(target);
  }
}
