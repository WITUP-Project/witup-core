package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.SummaryResolver;
import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jgrapht.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.LValue;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
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
  // for now, resolver being null means intraprocedural. fix me when poc is done
  private final SummaryResolver resolver;
  private static final Logger log = LoggerFactory.getLogger("BackwardSymbolicGenerator");

  public BackwardSymbolicGenerator(
      final WITUpGraph cpg, final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths) {
    this(cpg, constraintPaths, null);
  }

  public BackwardSymbolicGenerator(
      final WITUpGraph cpg,
      final List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths,
      final SummaryResolver resolver) {
    this.cpg = cpg;
    this.constraintPaths = constraintPaths;
    this.resolver = resolver;
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

      if (symExpr.getKind() == SymKind.BOOLEAN_METHOD) {
        truthValue = !truthValue;
      }

      symbolicConstraints.add(new SymbolicConstraint(symExpr, truthValue));
    }
    return symbolicConstraints;
  }

  private void setCurrentPath(final GraphPath<WITUpNode, WITUpEdge> p) {
    this.currentPath = p;
  }

  private SymExpr resolveAndSimplify(final SymExpr initial, final WITUpNode startNode) {
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), false);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    return SymExpr.stripBooleanEncoding(symExpr);
  }

  private SymExpr resolveAndSimplifyWithParams(final SymExpr initial, final WITUpNode startNode) {
    SymExpr symExpr = backwardSubstitute(initial, startNode, new HashSet<>(), true);
    symExpr = SymExpr.simplifyCmpPatterns(symExpr);
    return SymExpr.stripBooleanEncoding(symExpr);
  }

  public SymExpr generateSymbolicExpression(final WITUpNode constraintNode) {
    StmtGraphNode n = (StmtGraphNode) constraintNode.getNode();
    JIfStmt ifStmt = (JIfStmt) n.getStmt();
    return resolveAndSimplify(SymExpr.fromJimple(ifStmt.getCondition()), constraintNode);
  }

  public SymExpr generateReturnExpression(final ReturnStatementNode returnNode) {
    List<GraphPath<WITUpNode, WITUpEdge>> paths = cpg.getAllPathsToReturn(returnNode);
    if (paths.isEmpty()) {
      return SymExpr.fromJimple(returnNode.getOp());
    }
    this.currentPath = paths.get(0);
    return resolveAndSimplifyWithParams(SymExpr.fromJimple(returnNode.getOp()), returnNode);
  }

  private SymExpr backwardSubstitute(
      final SymExpr symExpr, final WITUpNode currentNode, final Set<WITUpNode> visited) {
    return backwardSubstitute(symExpr, currentNode, visited, false);
  }

  // it's ok to reassign current in a recursive function
  // given a Jimple statement, produces a SymExpr by backwards
  // tracing temporaries back to their origins
  private SymExpr backwardSubstitute(
      SymExpr symExpr, // SUPPRESS CHECKSTYLE FinalParameters
      final WITUpNode currentNode,
      final Set<WITUpNode> visited,
      final boolean followIdentity) {

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

      if (!isNodeInPath(sourceNode)) {
        continue;
      }

      if (!(sourceNode instanceof SimpleNode simpleNode)) {
        continue;
      }

      if (!(simpleNode.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }

      Stmt stmt = stmtNode.getStmt();

      Value lhsOp;
      Value rhsOp;

      if (stmt instanceof JAssignStmt assign) {
        if (!isStackVariable(assign.getLeftOp()) && assign.getRightOp() instanceof JCastExpr) {
          continue;
        }
        lhsOp = assign.getLeftOp();
        rhsOp = assign.getRightOp();
      } else if (stmt instanceof JIdentityStmt identity) {
        if (!followIdentity) {
          continue;
        }
        lhsOp = identity.getLeftOp();
        rhsOp = identity.getRightOp();
      } else {
        continue;
      }
      // local variable on the lhs e.g. $stack1 == 0
      String definedVar = getVariableName(lhsOp);

      if (!freeVars.contains(definedVar)) {
        continue;
      }

      // this is the interprocedural hook
      SymExpr rhsSymExpr = SymExpr.fromJimple(rhsOp);
      // if rhs is a virtual invoke and we have a resolver, try to substitute
      if (rhsSymExpr instanceof SymVirtualInvoke inv && resolver != null) {
        // extract callee signature and actuals from the Jimple invoke expr
        log.debug(
            "Interprocedural call site: {} — resolver present, substitution not yet implemented",
            inv);
      }

      symExpr = symExpr.substitute(definedVar, rhsSymExpr);
      symExpr = backwardSubstitute(symExpr, sourceNode, visited);
    }

    return symExpr;
  }

  //  private SymExpr backwardSubstituteUnbounded(
  //      SymExpr symExpr, final WITUpNode currentNode, final Set<WITUpNode> visited) {
  //
  //    if (visited.contains(currentNode)) {
  //      return symExpr;
  //    }
  //    visited.add(currentNode);
  //
  //    Set<String> freeVars = new VariableCollector().collect(symExpr);
  //    if (freeVars.isEmpty()) {
  //      return symExpr;
  //    }
  //
  //    for (DataDependencyEdge edge : cpg.getIncomingDDGEdges(currentNode)) {
  //      WITUpNode sourceNode = cpg.getEdgeSource(edge);
  //
  //      if (!(sourceNode instanceof SimpleNode simpleNode)) {
  //        continue;
  //      }
  //      if (!(simpleNode.getNode() instanceof StmtGraphNode stmtNode)) {
  //        continue;
  //      }
  //
  //      Stmt stmt = stmtNode.getStmt();
  //      if (!(stmt instanceof JAssignStmt assign)) {
  //        continue;
  //      }
  //      if (!isStackVariable(assign.getLeftOp()) && assign.getRightOp() instanceof JCastExpr) {
  //        continue;
  //      }
  //
  //      String definedVar = getVariableName(assign.getLeftOp());
  //      if (!freeVars.contains(definedVar)) {
  //        continue;
  //      }
  //
  //      SymExpr rhsSymExpr = SymExpr.fromJimple(assign.getRightOp());
  //      symExpr = symExpr.substitute(definedVar, rhsSymExpr);
  //      symExpr = backwardSubstituteUnbounded(symExpr, sourceNode, visited);
  //    }
  //    return symExpr;
  //  }

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
