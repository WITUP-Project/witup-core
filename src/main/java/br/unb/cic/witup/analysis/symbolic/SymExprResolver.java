package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.stmt.JAssignStmt;

// Bridges WITUpGraph (graph topology) and SymExpr (symbolic expressions) for resolving
// Soot-generated stack/var temps to the source-level expressions that defined them. Each
// nested SymVar with a stack_/var_ prefix is looked up via transitive DDG walk from the
// given site, substituted with its defining RHS, and the process is iterated until no
// stack/var names remain (or MAX_ITERATIONS hits, which bounds pathological chains).
//
// Lives outside both layers so neither has to depend on the other; consumers that
// already touch both (MethodSummariser, ExceptionFlowWalker) invoke it where they
// convert call-site operands into symbolic form.
public final class SymExprResolver {
  private static final int MAX_ITERATIONS = 8;

  private SymExprResolver() {}

  public static SymExpr resolveLocalAt(
      final SymExpr initial, final WITUpNode at, final WITUpGraph cpg) {
    SymExpr expr = initial;
    for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
      Set<String> names = new HashSet<>();
      expr.collectVarNames(names);
      boolean changed = false;
      for (String name : names) {
        if (!name.startsWith(SymVar.STACK_PREFIX)
            && !name.startsWith(SymVar.VAR_PREFIX)
            && !name.startsWith(SymVar.LOCAL_PREFIX)) {
          continue;
        }
        SymExpr def = findDefTransitively(name, at, cpg);
        if (def == null) {
          continue;
        }
        expr = expr.substitute(name, def);
        changed = true;
      }
      if (!changed) {
        return expr;
      }
    }
    return expr;
  }

  // BFS the DDG backward from `at` looking for an assignment whose LHS matches `name`.
  // Returns the assignment's RHS as a SymExpr, or null if no def is reachable. Picks the
  // first def encountered — for branchy/loopy code with multiple reaching defs this is
  // an approximation, but the typical Soot-generated stack chain is linear.
  private static SymExpr findDefTransitively(
      final String name, final WITUpNode at, final WITUpGraph cpg) {
    Set<WITUpNode> visited = new HashSet<>();
    Deque<WITUpNode> worklist = new ArrayDeque<>();
    worklist.push(at);
    while (!worklist.isEmpty()) {
      WITUpNode node = worklist.pop();
      if (!visited.add(node)) {
        continue;
      }
      for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(node)) {
        WITUpNode src = cpg.getEdgeSource(edge);
        if (src instanceof SimpleNode sn
            && sn.getNode() instanceof StmtGraphNode stmtNode
            && stmtNode.getStmt() instanceof JAssignStmt assign
            && SymVar.nameOf(assign.getLeftOp()).equals(name)) {
          return SymExpr.fromJimple(assign.getRightOp());
        }
        if (!visited.contains(src)) {
          worklist.push(src);
        }
      }
    }
    return null;
  }
}
