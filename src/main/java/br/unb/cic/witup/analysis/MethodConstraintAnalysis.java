package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Given a method that throws, build the symbolic constraints for each path leading to a throw node
 */
public final class MethodConstraintAnalysis {
  private final WITUpGraph cpg;
  private final Map<WITUpNode, List<List<SymbolicConstraint>>> symbolicThrowConstraints =
      new HashMap<>();

  public static Map<String, MethodSummary> summariseAll(
      final Map<String, WITUpGraph> methodGraphs) {
    return methodGraphs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    new MethodConstraintAnalysis(entry.getValue()).summariseConstraintPaths()));
  }

  public MethodConstraintAnalysis(final WITUpGraph cpg) {
    this.cpg = cpg;
  }

  public List<List<SymbolicConstraint>> getSymbolicConstraintPaths(final WITUpNode throwNode) {
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

  public MethodSummary summariseConstraintPaths() {
    List<List<SymbolicConstraint>> paths =
        getThrowNodes().stream()
            .flatMap(node -> getSymbolicConstraintPaths(node).stream())
            .collect(Collectors.toList());

    return new MethodSummary(getMethodSignature(), paths);
  }
}
