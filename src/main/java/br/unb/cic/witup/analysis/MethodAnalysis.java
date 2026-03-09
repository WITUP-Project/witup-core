package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MethodAnalysis {
  private final WITUpGraph cpg;
  private final Map<WITUpNode, List<List<SymbolicConstraint>>> symbolicConstraintPaths =
      new HashMap<>();

  public MethodAnalysis(final WITUpGraph cpg) {
    this.cpg = cpg;
  }

  public List<List<SymbolicConstraint>> getSymbolicConstraintPaths(final WITUpNode throwNode) {
    return symbolicConstraintPaths.computeIfAbsent(
        throwNode,
        node -> {
          var constraintPaths = cpg.getConstraintPaths(node);
          BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);
          return sg.generateSymbolicConstraintPaths();
        });
  }

  public String getMethodSignature() {
    return cpg.getMethodSignature();
  }

  public List<WITUpNode> getThrowNodes() {
    return cpg.getThrowNodes();
  }

  public WITUpGraph getCpg() {
    return cpg;
  }
}
