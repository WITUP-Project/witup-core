package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MethodConstraintAnalysis {
  private final WITUpGraph cpg;
  private final Map<WITUpNode, List<List<SymbolicConstraint>>> symbolicConstraintPaths =
      new HashMap<>();

  public MethodConstraintAnalysis(final WITUpGraph cpg) {
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

  public MethodSummary summarise(final MethodConstraintAnalysis analysis) {
    String sig = analysis.getMethodSignature();

    List<ThrowCase> throwCases = new ArrayList<>();
    for (WITUpNode throwNode : analysis.getThrowNodes()) {
      for (List<SymbolicConstraint> path : analysis.getSymbolicConstraintPaths(throwNode)) {
        for (SymbolicConstraint sc : path) {
          throwCases.add(new ThrowCase(sc.getSymExpr(), null, sc.getTruthValue()));
        }
      }
    }

    return new MethodSummary(sig, throwCases);
  }
}
