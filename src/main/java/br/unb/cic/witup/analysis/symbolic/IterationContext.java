package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.graph.WITUpPath;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.stmt.JAssignStmt;

/**
 * Per-path iteration symbolization for self-updating variables (loop counters, accumulators).
 * Without this, both the loop-entry and loop-exit boolean checks of an unrolled body refer to the
 * same SymExpr for the loop counter and resolve to its initial def, producing contradictory
 * constraints on a path that traverses the loop. Not sure how this fits into more general loop
 * unrolling techniques and how much loop handling we need
 */
final class IterationContext {

  static final IterationContext EMPTY = new IterationContext(Map.of(), List.of());

  private final Map<String, IterVarPositions> infoByVar;
  private final List<SymbolicConstraint> linkPreconditions;

  private IterationContext(
      final Map<String, IterVarPositions> infoByVar,
      final List<SymbolicConstraint> linkPreconditions) {
    this.infoByVar = infoByVar;
    this.linkPreconditions = linkPreconditions;
  }

  List<SymbolicConstraint> linkPreconditions() {
    return linkPreconditions;
  }

  /**
   * Returns expr with each self-updating variable's references replaced by the iteration symbol
   * appropriate for a constraint anchored at forwardEdgeIndex on this path. No-op when no
   * self-update vars apply, so callers don't need to gate.
   */
  SymExpr rewriteAt(final SymExpr expr, final int forwardEdgeIndex) {
    if (infoByVar.isEmpty()) {
      return expr;
    }
    SymExpr result = expr;
    for (Map.Entry<String, IterVarPositions> entry : infoByVar.entrySet()) {
      int iter = entry.getValue().iterAt(forwardEdgeIndex);
      SymVar replacement = entry.getValue().iterVars.get(iter);
      result = result.substitute(entry.getKey(), replacement);
    }
    return result;
  }

  static IterationContext compute(final WITUpPath path) {
    List<WITUpNode> forwardNodes = path.forwardNodes();

    // Single forward pass classifies each assign on the path. We keep the *first*
    // initial def and *first* self-update RHS per variable; if a variable were both
    // self-updating and re-initialized on the same path the analyzer would already
    // be in territory where this approximation is best-effort. hoping that examples
    // in the projects we are analysing will guide us in defining the approach
    Map<String, List<Integer>> selfUpdatePositions = new HashMap<>();
    Map<String, SymExpr> initialValues = new HashMap<>();
    Map<String, SymExpr> selfUpdateRhs = new HashMap<>();
    Map<String, SymKind> kinds = new HashMap<>();

    for (int i = 0; i < forwardNodes.size(); i++) {
      WITUpNode node = forwardNodes.get(i);
      if (!(node instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      String var = SymVar.nameOf(assign.getLeftOp());
      SymExpr rhs = SymExpr.fromJimple(assign.getRightOp());
      kinds.putIfAbsent(var, SymExpr.fromJimple(assign.getLeftOp()).getKind());
      if (rhs.contains(var)) {
        selfUpdatePositions.computeIfAbsent(var, k -> new ArrayList<>()).add(i);
        selfUpdateRhs.putIfAbsent(var, rhs);
      } else {
        initialValues.putIfAbsent(var, rhs);
      }
    }

    if (selfUpdatePositions.isEmpty()) {
      return EMPTY;
    }

    Map<String, IterVarPositions> iterVarsEnv = new HashMap<>(selfUpdatePositions.size());
    List<SymbolicConstraint> linkPreconditions = new ArrayList<>();

    for (Map.Entry<String, List<Integer>> entry : selfUpdatePositions.entrySet()) {
      String var = entry.getKey();
      List<Integer> positions = entry.getValue();
      int k = positions.size();
      SymKind kind = kinds.get(var);

      // i#0 … i#K
      List<SymVar> iterVars = new ArrayList<>(k + 1);
      for (int idx = 0; idx <= k; idx++) {
        iterVars.add(SymVar.fresh(var + "#" + idx, kind));
      }

      // i#0 = initial value, when the path also carries an initial def. If absent
      // (e.g. variable is a parameter never re-initialised on this path), i#0 stays
      // a free SymVar — Z3 will solve for it directly.
      SymExpr initial = initialValues.get(var);
      if (initial != null) {
        linkPreconditions.add(
            new SymbolicConstraint(new SymBinOp(BinOp.EQ, iterVars.getFirst(), initial), true));
      }

      // i#{k+1} = step(i#k). step is the self-update RHS with `var` rebound to i#k.
      SymExpr step = selfUpdateRhs.get(var);
      for (int idx = 0; idx < k; idx++) {
        SymExpr rewrittenStep = step.substitute(var, iterVars.get(idx));
        linkPreconditions.add(
            new SymbolicConstraint(
                new SymBinOp(BinOp.EQ, iterVars.get(idx + 1), rewrittenStep), true));
      }

      iterVarsEnv.put(var, new IterVarPositions(iterVars, positions));
    }

    return new IterationContext(iterVarsEnv, linkPreconditions);
  }

  private static final class IterVarPositions {
    private final List<SymVar> iterVars;
    private final int[] selfUpdatePositions;

    IterVarPositions(final List<SymVar> iterVars, final List<Integer> positions) {
      this.iterVars = iterVars;
      this.selfUpdatePositions = positions.stream().mapToInt(Integer::intValue).toArray();
    }

    int iterAt(final int forwardEdgeIndex) {
      // Self-update at node-position p has applied by the time we reach an edge whose
      // source is at index >= p+1. Equivalently for our forward-edge anchor, p strictly
      // less than forwardEdgeIndex means the def's effect is visible.
      int count = 0;
      for (int pos : selfUpdatePositions) {
        if (pos < forwardEdgeIndex) {
          count++;
        } else {
          break; // positions are appended in forward order, so monotonic
        }
      }
      return count;
    }
  }
}
