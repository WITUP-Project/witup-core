package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.graph.CfgSccIndex;
import br.unb.cic.witup.analysis.graph.ImplicitNpeReceiverSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.ExceptionalCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.IfStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymNull;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;

/**
 * Walks a method forward from its entry, carrying conditions to get to each point maxUnrolls bounds
 * how far one fact may go round a loop. maxFactsPerNode} bounds how many ways of arriving at a
 * point
 */
public final class ForwardPathAnalysis {
  // constants we'll tweak empirically and report on
  public static final int DEFAULT_MAX_UNROLLS = 1;
  public static final int DEFAULT_MAX_FACTS_PER_NODE = 8;

  private final WITUpGraph cpg;
  private final CfgSccIndex scc;
  private final int maxFactsPerNode;
  private final int maxUnrolls;
  private final Map<WITUpNode, Set<PathFact>> state;
  private final Map<WITUpNode, List<Local>> dereferencedByNode;

  private ForwardPathAnalysis(
      final WITUpGraph cpg, final int maxFactsPerNode, final int maxUnrolls) {
    this.cpg = cpg;
    this.scc = cpg.sccIndex();
    this.maxFactsPerNode = maxFactsPerNode;
    this.maxUnrolls = maxUnrolls;
    this.state = new IdentityHashMap<>(cpg.vertexSet().size() * 2);
    this.dereferencedByNode = new IdentityHashMap<>();
    for (ImplicitNpeReceiverSite site : cpg.getImplicitNpeReceiverSites()) {
      dereferencedByNode.computeIfAbsent(site.node(), k -> new ArrayList<>()).add(site.receiver());
    }
  }

  /** Facts reaching each node of {@code interesting}, under the default budgets. */
  public static PathConditionIndex analyseMethodPaths(
      final WITUpGraph cpg, final Set<WITUpNode> relevant) {
    return analyseMethodPaths(cpg, relevant, DEFAULT_MAX_FACTS_PER_NODE, DEFAULT_MAX_UNROLLS);
  }

  /** Facts reaching each node of {@code interesting}. Other nodes are visited but not retained. */
  public static PathConditionIndex analyseMethodPaths(
      final WITUpGraph cpg,
      final Set<WITUpNode> interesting,
      final int maxFactsPerNode,
      final int maxUnrolls) {
    ForwardPathAnalysis analysis = new ForwardPathAnalysis(cpg, maxFactsPerNode, maxUnrolls);
    analysis.propagate();
    return analysis.project(interesting);
  }

  private void propagate() {
    int components = scc.topologicalSccs().size();
    Deque<WITUpNode> worklist = new ArrayDeque<>();
    for (WITUpNode root : cpg.getRootNodes()) {
      state.computeIfAbsent(root, k -> new LinkedHashSet<>()).add(PathFact.initial(components));
      worklist.add(root);
    }

    while (!worklist.isEmpty()) {
      WITUpNode node = worklist.poll();
      for (PathFact fact : new ArrayList<>(state.get(node))) {
        List<SymExpr> dereferenced = dereferencedAt(node, fact);
        PathFact leaving = applyStatement(node, fact);
        for (WITUpEdge edge : cpg.outgoingCfgEdges(node)) {
          PathFact arriving = applyEdge(edge, leaving, dereferenced);
          if (arriving != null && absorb(edge.getTarget(), arriving)) {
            worklist.add(edge.getTarget());
          }
        }
      }
    }
  }

  // Takes `arriving` into the state at `node`
  private boolean absorb(final WITUpNode node, final PathFact arriving) {
    Set<PathFact> facts = state.computeIfAbsent(node, k -> new LinkedHashSet<>());
    PathFact collapsed = facts.size() == 1 ? facts.iterator().next() : null;
    if (collapsed != null && collapsed.isWidened()) {
      PathFact merged = collapse(List.of(collapsed, arriving), node);
      if (merged.equals(collapsed)) {
        return false;
      }
      facts.clear();
      facts.add(merged);
      return true;
    }
    if (!facts.add(arriving)) {
      return false;
    }
    if (facts.size() > maxFactsPerNode) {
      PathFact merged = collapse(List.copyOf(facts), node);
      facts.clear();
      facts.add(merged);
    }
    return true;
  }

  private PathFact collapse(final List<PathFact> facts, final WITUpNode at) {
    List<PathCondition> conditions = facts.stream().map(PathFact::pc).toList();

    Set<String> names = new TreeSet<>();
    for (PathFact fact : facts) {
      names.addAll(fact.env().keySet());
    }

    Map<String, SymExpr> merged = new LinkedHashMap<>();
    for (String name : names) {
      SymExpr agreed = agreedBinding(facts, name);
      merged.put(name, agreed != null ? agreed : opaque(name, at, facts));
    }

    int[] unrolls = new int[scc.topologicalSccs().size()];
    for (PathFact fact : facts) {
      for (int component = 0; component < unrolls.length; component++) {
        unrolls[component] = Math.max(unrolls[component], fact.unrollsOf(component));
      }
    }
    return PathFact.collapsed(PathCondition.intersect(conditions), merged, unrolls);
  }

  private static SymExpr agreedBinding(final List<PathFact> facts, final String name) {
    SymExpr agreed = facts.getFirst().env().get(name);
    if (agreed == null) {
      return null;
    }
    for (PathFact fact : facts) {
      if (!agreed.equals(fact.env().get(name))) {
        return null;
      }
    }
    return agreed;
  }

  private SymExpr opaque(final String name, final WITUpNode at, final List<PathFact> facts) {
    SymKind kind = SymKind.OTHER;
    for (PathFact fact : facts) {
      SymExpr bound = fact.env().get(name);
      if (bound != null) {
        kind = bound.getKind();
        break;
      }
    }
    return SymVar.fresh(name + "$" + scc.positionOf(at), kind);
  }

  private PathConditionIndex project(final Set<WITUpNode> interesting) {
    Map<WITUpNode, List<PathFact>> retained = new IdentityHashMap<>(interesting.size() * 2);
    for (WITUpNode node : interesting) {
      Set<PathFact> facts = state.get(node);
      if (facts != null && !facts.isEmpty()) {
        retained.put(node, List.copyOf(facts));
      }
    }
    return PathConditionIndex.of(retained);
  }

  private static PathFact applyStatement(final WITUpNode node, final PathFact fact) {
    Stmt stmt = statementOf(node);
    if (stmt instanceof JIdentityStmt identity) {
      return bindEntryValue(identity, fact);
    }
    if (stmt instanceof JAssignStmt assign && assign.getLeftOp() instanceof Local) {
      String name = SymVar.nameOf(assign.getLeftOp());
      SymExpr value =
          SymbolicConstraintGenerator.foldConstants(
              SymExpr.fromJimple(assign.getRightOp()).resolveWith(fact.env()));
      return fact.withBinding(name, value);
    }
    // assignments into an array element or a field change the heap, which is not modelled.
    return fact;
  }

  // `@this` is left unbound on purpose: binding it turns a field base that was
  // an Int-sorted variable into a receiver-sorted one, and Z3 then sees the same field accessor
  // declared over one sort and applied over another
  // `@caughtexception` is left to the machinery that already seals it.
  private static PathFact bindEntryValue(final JIdentityStmt identity, final PathFact fact) {
    if (!(identity.getRightOp() instanceof JParameterRef parameter)) {
      return fact;
    }
    String name = SymVar.nameOf(identity.getLeftOp());
    SymExpr bound = SymExpr.fromJimple(parameter);
    return fact.withBinding(name, bound instanceof SymParamRef ref ? ref.withName(name) : bound);
  }

  private PathFact applyEdge(
      final WITUpEdge edge, final PathFact fact, final List<SymExpr> dereferenced) {
    PathFact carried = provenNonNull(fact, edge, dereferenced);
    if (isBackEdge(edge)) {
      int component = scc.sccIdOf(edge.getTarget());
      if (carried.unrollsOf(component) >= maxUnrolls) {
        return null;
      }
      carried = carried.withUnroll(component);
    }
    if (!(edge instanceof BooleanCFGEdge branch)
        || !(edge.getSource() instanceof IfStatementNode ifNode)) {
      return carried;
    }
    SymExpr condition =
        SymbolicConstraintGenerator.foldConstants(
            SymExpr.fromJimple(ifNode.getCondition()).resolveWith(carried.env()));
    if (condition instanceof SymIntConst constant) {
      // The environment settled the condition, so one arm is unreachable from this fact
      return (constant.getValue() != 0) == branch.getCondition() ? carried : null;
    }
    return provenByInstanceOf(
        carried.withConstraint(new SymbolicConstraint(condition, branch.getCondition())),
        condition,
        branch.getCondition());
  }

  /**
   * instanceof imlies non-null
   */
  private static PathFact provenByInstanceOf(
      final PathFact fact, final SymExpr condition, final boolean edgeTaken) {
    SymExpr subject = condition;
    boolean holds = edgeTaken;
    if (subject instanceof SymBinOp comparison
        && comparison.getRhs() instanceof SymIntConst zero
        && zero.getValue() == 0) {
      if (comparison.getOp() == BinOp.EQ) {
        holds = !holds;
      } else if (comparison.getOp() != BinOp.NE) {
        return fact;
      }
      subject = comparison.getLhs();
    }
    if (!holds || !(subject instanceof SymInstanceOf test)) {
      return fact;
    }
    SymExpr value = SymbolicConstraintGenerator.foldConstants(test.getOp().resolveWith(fact.env()));
    SymExpr assertion =
        SymbolicConstraintGenerator.foldConstants(new SymBinOp(BinOp.NE, value, SymNull.INSTANCE));
    if (assertion instanceof SymIntConst) {
      return fact;
    }
    SymbolicConstraint constraint = new SymbolicConstraint(assertion, true);
    return fact.pc().contains(constraint) ? fact : fact.withConstraint(constraint);
  }

  private List<SymExpr> dereferencedAt(final WITUpNode node, final PathFact fact) {
    List<Local> locals = dereferencedByNode.get(node);
    if (locals == null) {
      return List.of();
    }
    List<SymExpr> resolved = new ArrayList<>(locals.size());
    for (Local local : locals) {
      resolved.add(
          SymbolicConstraintGenerator.foldConstants(
              SymExpr.fromJimple(local).resolveWith(fact.env())));
    }
    return resolved;
  }

  /** models the effect of a successfull dereference */
  private static PathFact provenNonNull(
      final PathFact fact, final WITUpEdge edge, final List<SymExpr> dereferenced) {
    if (dereferenced.isEmpty() || edge instanceof ExceptionalCFGEdge) {
      return fact;
    }
    PathFact carried = fact;
    for (SymExpr value : dereferenced) {
      SymExpr assertion =
          SymbolicConstraintGenerator.foldConstants(
              new SymBinOp(BinOp.NE, value, SymNull.INSTANCE));
      if (assertion instanceof SymIntConst) {
        continue;
      }
      SymbolicConstraint constraint = new SymbolicConstraint(assertion, true);
      if (!carried.pc().contains(constraint)) {
        carried = carried.withConstraint(constraint);
      }
    }
    return carried;
  }

  private boolean isBackEdge(final WITUpEdge edge) {
    if (!scc.isIntraScc(edge)) {
      return false;
    }
    List<WITUpNode> component = scc.topologicalSccs().get(scc.sccIdOf(edge.getTarget()));
    return component.getFirst() == edge.getTarget();
  }

  private static Stmt statementOf(final WITUpNode node) {
    return node.getNode() instanceof StmtGraphNode stmtNode ? stmtNode.getStmt() : null;
  }
}
