package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.SimpleNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jgrapht.GraphPath;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;

public final class PathResolver {
  private final WITUpGraph cpg;
  private final Map<String, SymKind> symbolKindTable;
  private final List<GraphPath<WITUpNode, WITUpEdge>> paths;
  private final Set<String> variableSet;
  private GraphPath<WITUpNode, WITUpEdge> currentPath;

  public PathResolver(final WITUpGraph cpg, final List<GraphPath<WITUpNode, WITUpEdge>> paths) {
    this.cpg = cpg;
    symbolKindTable = new HashMap<>();
    variableSet = new HashSet<>();
    this.paths = paths;
  }

  public List<List<ResolvedThrowCondition>> resolveConditionPaths() {
    List<List<ResolvedThrowCondition>> resolvedThrowConditions = new ArrayList<>();
    for (GraphPath<WITUpNode, WITUpEdge> p : this.paths) {
      List<ResolvedThrowCondition> resolved = resolveConditionPath(p);

      if (!resolved.isEmpty()) {
        resolvedThrowConditions.add(resolved);
      }
    }
    return resolvedThrowConditions;
  }

  public List<ResolvedThrowCondition> resolveConditionPath(
      final GraphPath<WITUpNode, WITUpEdge> p) {

    this.setCurrentPath(p);
    List<ResolvedThrowCondition> resolvedThrowConditions = new ArrayList<>();
    for (ThrowCondition throwCondition : cpg.getThrowConditions(p)) {
      SymExpr resolved = resolveThrowCondition(throwCondition.getNode());
      boolean truthValue = throwCondition.getTruthValue();

      if (resolved.kind() == SymKind.BOOLEAN_METHOD) {
        truthValue = !truthValue;
      }

      resolvedThrowConditions.add(new ResolvedThrowCondition(resolved, truthValue));
    }
    return resolvedThrowConditions;
  }

  private void setCurrentPath(GraphPath<WITUpNode, WITUpEdge> p) {
    this.currentPath = p;
  }

  public SymExpr resolveThrowCondition(final WITUpNode ifNode) {
    StmtGraphNode n = (StmtGraphNode) ifNode.getNode();
    JIfStmt ifStmt = (JIfStmt) n.getStmt();
    SymExpr condition = SymExpr.fromValue(ifStmt.getCondition());

    collectVariables(condition);

    // traverse backward and substitute
    SymExpr resolved = resolveVariables(condition, ifNode, new HashSet<>());

    resolved = SymExpr.simplifyCmpPatterns(resolved);
    resolved = SymExpr.stripBooleanEncoding(resolved);
    collectSymbolKinds(resolved);

    return resolved;
  }
  
  private void collectVariables(final SymExpr expr) {
    variableSet.addAll(new VariableCollector().collect(expr));
  }

  // it's ok to reassign current in a recursive function
  private SymExpr resolveVariables(
      SymExpr symExpr, // SUPPRESS CHECKSTYLE FinalParameters
      final WITUpNode currentNode,
      final Set<WITUpNode> visited) {
    if (variableSet.isEmpty()) {
      return symExpr;
    }

    if (visited.contains(currentNode)) {
      return symExpr;
    }
    visited.add(currentNode);

    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
      WITUpNode sourceNode = cpg.getEdgeSource(edge);

      // do not resolve data dependency edges that are not in the current path.
      if (!isNodeInPath(sourceNode)) {
        continue;
      }

      if (!(sourceNode instanceof SimpleNode simpleNode)) {
        continue;
      }

      PropertyGraphNode propNode = simpleNode.getNode();
      if (!(simpleNode.getNode() instanceof StmtGraphNode)) {
        continue;
      }

      StmtGraphNode stmtNode = (StmtGraphNode) propNode;
      Stmt stmt = stmtNode.getStmt();

      if (!(stmt instanceof JAssignStmt assign)) {
        continue;
      }

      Value leftOp = assign.getLeftOp();
      // local variable on the lhs e.g. $stack1 == 0
      String definedVar = getVariableName(leftOp);

      if (variableSet.contains(definedVar)) {
        // translate the RHS to symbolic expression
        SymExpr rhsSymExpr = SymExpr.fromValue(assign.getRightOp());

        // substitute this variable in our current expression
        symExpr = symExpr.substitute(definedVar, rhsSymExpr);

        variableSet.remove(definedVar);
        collectVariables(rhsSymExpr);
        symExpr = resolveVariables(symExpr, sourceNode, visited);
      }
    }

    return symExpr;
  }

  private boolean isNodeInPath(final WITUpNode node) {
    PropertyGraphNode targetNode = node.getNode();

    Set<WITUpNode> nodesInPath = new HashSet<>(this.getCurrentPath().getVertexList());
    for (WITUpNode pathNode : nodesInPath) {
      if (pathNode.getNode().equals(targetNode)) {
        return true;
      }
    }

    return false;
  }

  private static String getVariableName(final Value value) {
    return value.toString();
  }

  private GraphPath<WITUpNode, WITUpEdge> getCurrentPath() {
    return this.currentPath;
  }

  public Map<String, SymKind> getSymbolKindTable() {
    return symbolKindTable;
  }

  private void collectSymbolKinds(final SymExpr expr) {
    symbolKindTable.putAll(new SymbolKindCollector().collect(expr));
  }
}
