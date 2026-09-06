package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;

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
        SymExpr def = findDefTransitively(name, at, cpg, isSootTemp(name));
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

  private static boolean isSootTemp(final String name) {
    return name.startsWith(SymVar.STACK_PREFIX)
        || name.startsWith(SymVar.VAR_PREFIX)
        || name.startsWith(SymVar.LOCAL_PREFIX);
  }

  // BFS the DDG backward from `at` looking for a def of `name`.
  //
  // Parameter bindings (`s := @parameter0`) are the method's single unconditional entry defs,
  // so it is safe to resolve when there is no shadowing
  private static SymExpr findDefTransitively(
      final String name,
      final WITUpNode at,
      final WITUpGraph cpg,
      final boolean followAssignments) {
    Set<WITUpNode> visited = new HashSet<>();
    Deque<WITUpNode> worklist = new ArrayDeque<>();
    worklist.push(at);
    SymExpr entryBinding = null;
    boolean assignShadows = false;
    while (!worklist.isEmpty()) {
      WITUpNode node = worklist.pop();
      if (!visited.add(node)) {
        continue;
      }
      for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(node)) {
        WITUpNode src = cpg.getEdgeSource(edge);
        Stmt stmt = stmtOf(src);
        if (stmt instanceof JAssignStmt assign && SymVar.nameOf(assign.getLeftOp()).equals(name)) {
          if (followAssignments) {
            return SymExpr.fromJimple(assign.getRightOp());
          }
          assignShadows = true;
        } else if (stmt instanceof JIdentityStmt identity
            && SymVar.nameOf(identity.getLeftOp()).equals(name)
            && isEntryBinding(identity.getRightOp())) {
          entryBinding = nameIfParam(SymExpr.fromJimple(identity.getRightOp()), name);
        }
        if (!visited.contains(src)) {
          worklist.push(src);
        }
      }
    }
    return assignShadows ? null : entryBinding;
  }

  private static Stmt stmtOf(final WITUpNode node) {
    return node.getNode() instanceof StmtGraphNode stmtNode ? stmtNode.getStmt() : null;
  }

  private static boolean isEntryBinding(final Value rightOp) {
    return rightOp instanceof JParameterRef;
  }

  private static SymExpr nameIfParam(final SymExpr expr, final String sourceName) {
    return expr instanceof SymParamRef paramRef ? paramRef.withName(sourceName) : expr;
  }
}
