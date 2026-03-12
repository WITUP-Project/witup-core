package br.unb.cic.witup.test;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import org.jgrapht.GraphPath;

import java.util.ArrayList;
import java.util.List;

public final class SymbolicTestHelper {
  private SymbolicTestHelper() {}

  public static List<SolverResult> solve(
          final WITUpGraph cpg,
          final WITUpNode throwNode,
          final String methodSignature) {
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getConstraintPaths(throwNode);
    List<List<SymbolicConstraint>> symbolicPaths =
            new BackwardSymbolicGenerator(cpg, paths).generateSymbolicConstraintPaths();
    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicPaths.size(); i++) {
      results.add(solver.checkPath(methodSignature + "#" + i, symbolicPaths.get(i)));
    }
    return results;
  }
}
