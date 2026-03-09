package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jgrapht.GraphPath;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;

/**
 * Translates Jimple constraints into SymbolicConstraints. Uses backwards data flow resolution to
 * trace temporaries, parameters back to their origin node Produces a path of symbolic constraints
 * to be tested by Z3.
 */
public final class BackwardSymbolicGenerator {
  private final WITUpGraph cpg;
  private final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths;
  private GraphPath<WITUpNode, WITUpEdge> currentPath;

  public BackwardSymbolicGenerator(
      final WITUpGraph cpg, final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths) {
    this.cpg = cpg;
    this.constraintPaths = constraintPaths;
  }

  public List<List<SymbolicConstraint>> generateSymbolicConstraintPaths() {
    List<List<SymbolicConstraint>> symbolicConstraints = new ArrayList<>();
    for (GraphPath<WITUpNode, WITUpEdge> p : this.constraintPaths) {
      List<SymbolicConstraint> resolved = generateSymbolicConstraints(p);

      if (!resolved.isEmpty()) {
        symbolicConstraints.add(resolved);
      }
    }
    return symbolicConstraints;
  }

  public List<SymbolicConstraint> generateSymbolicConstraints(
      final GraphPath<WITUpNode, WITUpEdge> p) {

    this.setCurrentPath(p);
    List<SymbolicConstraint> symbolicConstraints = new ArrayList<>();
    for (ThrowConstraint throwConstraint : cpg.getThrowConstraints(p)) {
      SymExpr symExpr = generateSymbolicExpression(throwConstraint.node());
      boolean truthValue = throwConstraint.truthValue();

      if (symExpr.kind() == SymKind.BOOLEAN_METHOD) {
        truthValue = !truthValue;
      }

      symbolicConstraints.add(new SymbolicConstraint(symExpr, truthValue));
    }
    return symbolicConstraints;
  }

  private void setCurrentPath(final GraphPath<WITUpNode, WITUpEdge> p) {
    this.currentPath = p;
  }

  public SymExpr generateSymbolicExpression(final WITUpNode constraintNode) {
    StmtGraphNode n = (StmtGraphNode) constraintNode.getNode();
    JIfStmt ifStmt = (JIfStmt) n.getStmt();
    SymExpr symExpr = SymExpr.fromValue(ifStmt.getCondition());

    // traverse backward and substitute temporaries so that each SymbolicConstraint
    // element has all the information it needs to pass to a solver
    symExpr = backwardSubstitute(symExpr, constraintNode, new HashSet<>());

    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    symExpr = SymExpr.stripBooleanEncoding(symExpr);

    return symExpr;
  }

  // it's ok to reassign current in a recursive function
  // given a Jimple statement, produces a SymExpr by backwards
  // tracing temporaries back to their origins
  private SymExpr backwardSubstitute(
      SymExpr symExpr, // SUPPRESS CHECKSTYLE FinalParameters
      final WITUpNode currentNode,
      final Set<WITUpNode> visited) {

    if (visited.contains(currentNode)) {
      return symExpr;
    }
    visited.add(currentNode);

    Set<String> freeVars = new VariableCollector().collect(symExpr);

    if (freeVars.isEmpty()) {
      return symExpr;
    }

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

      if (!isStackVariable(assign.getLeftOp()) && assign.getRightOp() instanceof JCastExpr) {
        continue;
      }

      Value leftOp = assign.getLeftOp();
      // local variable on the lhs e.g. $stack1 == 0
      String definedVar = getVariableName(leftOp);

      if (!freeVars.contains(definedVar)) {
        continue;
      }

      SymExpr rhsSymExpr = SymExpr.fromValue(assign.getRightOp());
      symExpr = symExpr.substitute(definedVar, rhsSymExpr);
      symExpr = backwardSubstitute(symExpr, sourceNode, visited);
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

  private boolean isStackVariable(final LValue value) {
    return value.toString().contains("$stack");
  }
}
