package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MethodConstraintAnalysis {
  private final WITUpGraph cpg;
  private final Map<WITUpNode, List<List<SymbolicConstraint>>>
          symbolicThrowConstraints = new HashMap<>();

  public MethodConstraintAnalysis(final WITUpGraph cpg) {
    this.cpg = cpg;
  }

  public List<List<SymbolicConstraint>>
      getSymbolicConstraintPaths(final WITUpNode throwNode) {
    return symbolicThrowConstraints.computeIfAbsent(
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

  public MethodSummary summarise(final MethodConstraintAnalysis analysis) {
    List<SymbolicConstraint> constraints = analysis.getThrowNodes().stream()
            .flatMap(node -> analysis.getSymbolicConstraintPaths(node).stream())
            .flatMap(List::stream)
            .collect(Collectors.toList());

    return new MethodSummary(analysis.getMethodSignature(), constraints);
  }
}
