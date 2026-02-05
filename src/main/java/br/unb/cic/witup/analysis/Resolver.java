package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.SimpleNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.jimple.common.constant.FloatConstant;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JCmpExpr;
import sootup.core.jimple.common.expr.JCmpgExpr;
import sootup.core.jimple.common.expr.JCmplExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;

public final class Resolver {
  private final WITUpGraph ddg;

  public Resolver(final WITUpGraph ddg) {
    this.ddg = ddg;
  }

  public List<ResolvedThrowCondition> resolveConditionPath(
      final List<ThrowCondition> throwConditionsPath) {
    List<ResolvedThrowCondition> resolvedThrowConditions = new ArrayList<>();
    for (ThrowCondition throwCondition : throwConditionsPath) {
      SymExpr resolved = resolveThrowCondition(throwCondition.getNode());
      resolvedThrowConditions.add(
          new ResolvedThrowCondition(resolved, throwCondition.getTruthValue()));
    }
    return resolvedThrowConditions;
  }

  public List<List<ResolvedThrowCondition>> resolveConditionPaths(
      final List<List<ThrowCondition>> throwConditionsPaths) {
    List<List<ResolvedThrowCondition>> resolvedThrowConditions = new ArrayList<>();
    for (List<ThrowCondition> throwConditionsPath : throwConditionsPaths) {
      resolvedThrowConditions.add(resolveConditionPath(throwConditionsPath));
    }
    return resolvedThrowConditions;
  }

  /**
   * Starting from a condition node (e.g. $stack2 >= 0), walk backward through the DDG and build a
   * symbolic expression by substituting the locals.
   *
   * @param ifNode ifNode The if statement node that guards the throw
   * @return Symbolic expression representing the condition
   */
  public SymExpr resolveThrowCondition(final WITUpNode ifNode) {
    StmtGraphNode n = (StmtGraphNode) ifNode.getNode();
    JIfStmt ifStmt = (JIfStmt) n.getStmt();
    SymExpr condition = valueToSymExpr(ifStmt.getCondition());

    Set<String> varsToResolve = findVariables(condition);

    // traverse backward and substitute
    SymExpr resolved = resolveVariables(condition, varsToResolve, ifNode, ddg, new HashSet<>());

    resolved = simplifyCmpPatterns(resolved);

    return resolved;
  }

  /** Simplify patterns like (x cmpg y) >= 0 to x >= y */
  private static SymExpr simplifyCmpPatterns(final SymExpr expr) {
    if (!(expr instanceof SymBinOp binOp)) {
      return expr;
    }

    SymExpr left = simplifyCmpPatterns(binOp.getLeft());
    SymExpr right = simplifyCmpPatterns(binOp.getRight());

    // Pattern: (x cmpg/cmpl y) op 0
    if (left instanceof SymBinOp && right instanceof SymConst) {
      SymBinOp leftBinOp = (SymBinOp) left;
      SymConst rightConst = (SymConst) right;

      // Check if it's a cmp operation compared to 0. Not sure if we need
      // to handle comparisons with numbers other than 0
      if ((leftBinOp.getOp() == BinOp.CMPG
              || leftBinOp.getOp() == BinOp.CMPL
              || leftBinOp.getOp() == BinOp.CMP)
          && rightConst.getValue().equals(0)) {

        // cmpg/cmpl returns: -1 if left < right, 0 if equal, 1 if left > right
        // So: (x cmpg y) >= 0 means x >= y
        //     (x cmpg y) > 0 means x > y
        //     (x cmpg y) == 0 means x == y
        //     (x cmpg y) < 0 means x < y
        //     (x cmpg y) <= 0 means x <= y

        return new SymBinOp(binOp.getOp(), leftBinOp.getLeft(), leftBinOp.getRight());
      }
    }

    // Return with simplified children
    if (left != binOp.getLeft() || right != binOp.getRight()) {
      return new SymBinOp(binOp.getOp(), left, right);
    }

    return expr;
  }

  /** Find all variable names in an expression */
  private static Set<String> findVariables(final SymExpr expr) {
    Set<String> vars = new HashSet<>();
    collectVariables(expr, vars);
    return vars;
  }

  private static void collectVariables(final SymExpr expr, final Set<String> vars) {
    if (expr instanceof SymVar) {
      vars.add(((SymVar) expr).getName());
    } else if (expr instanceof SymBinOp) {
      collectVariables(((SymBinOp) expr).getLeft(), vars);
      collectVariables(((SymBinOp) expr).getRight(), vars);
    } else if (expr instanceof SymField) {
      collectVariables(((SymField) expr).getBase(), vars);
    }
  }

  /** Get variable name from a Value (for assignment LHS) */
  private static String getVariableName(final Value value) {
    if (value instanceof Local) {
      return value.toString();
    }
    return value.toString(); // Fallback
  }

  /** Map Jimple comparison operators to BinOp */
  private static BinOp jimpleOpToBinOp(final AbstractConditionExpr expr) {
    if (expr instanceof JEqExpr) {
      return BinOp.EQ;
    }
    if (expr instanceof JNeExpr) {
      return BinOp.NE;
    }
    if (expr instanceof JLtExpr) {
      return BinOp.LT;
    }
    if (expr instanceof JLeExpr) {
      return BinOp.LE;
    }
    if (expr instanceof JGtExpr) {
      return BinOp.GT;
    }
    if (expr instanceof JGeExpr) {
      return BinOp.GE;
    }
    throw new IllegalArgumentException("Unknown condition expr: " + expr.getClass());
  }

  /** Map Jimple binary operators to BinOp */
  private static BinOp jimpleBinopToBinOp(final AbstractBinopExpr expr) {
    if (expr instanceof JAddExpr) {
      return BinOp.ADD;
    }
    if (expr instanceof JSubExpr) {
      return BinOp.SUB;
    }
    if (expr instanceof JMulExpr) {
      return BinOp.MUL;
    }
    if (expr instanceof JDivExpr) {
      return BinOp.DIV;
    }
    if (expr instanceof JRemExpr) {
      return BinOp.MOD;
    }
    if (expr instanceof JCmpExpr) {
      return BinOp.CMP;
    }
    if (expr instanceof JCmpgExpr) {
      return BinOp.CMPG;
    }
    if (expr instanceof JCmplExpr) {
      return BinOp.CMPL;
    }
    throw new IllegalArgumentException("Unknown binop expr: " + expr.getClass());
  }

  /** Resolve variables by traversing backward through DDG */
  // it's ok to reassign current in a recursive function
  private static SymExpr resolveVariables(
      SymExpr current, // SUPPRESS CHECKSTYLE FinalParameters
      final Set<String> varsToResolve,
      final WITUpNode currentNode,
      final WITUpGraph graph,
      final Set<WITUpNode> visited) {
    if (varsToResolve.isEmpty()) {
      return current; // All resolved!
    }

    if (visited.contains(currentNode)) {
      return current; // Avoid cycles
    }
    visited.add(currentNode);

    // Find incoming data dependency edges
    List<DataDependencyEdge> incomingDDG = getIncomingDDGEdges(currentNode, graph);

    for (DataDependencyEdge edge : incomingDDG) {
      WITUpNode sourceNode = graph.getEdgeSource(edge);

      // Check if this node defines any variable we need
      if (sourceNode instanceof SimpleNode) {
        SimpleNode simpleNode = (SimpleNode) sourceNode;
        PropertyGraphNode propNode = simpleNode.getNode();

        if (!(propNode instanceof StmtGraphNode)) {
          continue;
        }

        StmtGraphNode stmtNode = (StmtGraphNode) propNode;
        Stmt stmt = stmtNode.getStmt();

        if (stmt instanceof JAssignStmt) {
          JAssignStmt assign = (JAssignStmt) stmt;
          Value leftOp = assign.getLeftOp();

          // Get the variable name being defined
          String definedVar = getVariableName(leftOp);

          // If this is a variable we need to resolve
          if (varsToResolve.contains(definedVar)) {
            // Translate the RHS to symbolic expression
            SymExpr rhsExpr = valueToSymExpr(assign.getRightOp());

            // Substitute this variable in our current expression
            current = current.substitute(definedVar, rhsExpr);

            // Remove from vars to resolve
            varsToResolve.remove(definedVar);

            // Add any new variables introduced by RHS
            varsToResolve.addAll(findVariables(rhsExpr));

            // Continue resolving from this node
            current = resolveVariables(current, varsToResolve, sourceNode, graph, visited);
          }
        }
      }
    }

    return current;
  }

  /** Convert a Jimple Value to SymExpr */
  private static SymExpr valueToSymExpr(final Value value) {
    // eg $stack1
    if (value instanceof Local) {
      return new SymVar(value.toString());
    }

    if (value instanceof IntConstant) {
      return new SymConst(((IntConstant) value).getValue());
    }
    if (value instanceof DoubleConstant) {
      return new SymConst(((DoubleConstant) value).getValue());
    }
    if (value instanceof FloatConstant) {
      return new SymConst(((FloatConstant) value).getValue());
    }
    if (value instanceof LongConstant) {
      return new SymConst(((LongConstant) value).getValue());
    }
    if (value instanceof StringConstant) {
      return new SymConst(((StringConstant) value).getValue());
    }
    if (value instanceof NullConstant) {
      return new SymConst(null);
    }

    // eg this.radius
    if (value instanceof JFieldRef) {
      JFieldRef fieldRef = (JFieldRef) value;
      // For instance fields: this.<ClassName: type fieldName>
      if (fieldRef instanceof JInstanceFieldRef) {
        JInstanceFieldRef instField = (JInstanceFieldRef) fieldRef;
        SymExpr base = valueToSymExpr(instField.getBase());
        String fieldName = instField.getFieldSignature().getName();
        return new SymField(base, fieldName);
      }
    }

    // Binary operations
    if (value instanceof AbstractConditionExpr) {
      AbstractConditionExpr condExpr = (AbstractConditionExpr) value;
      SymExpr left = valueToSymExpr(condExpr.getOp1());
      SymExpr right = valueToSymExpr(condExpr.getOp2());
      BinOp op = jimpleOpToBinOp(condExpr);
      return new SymBinOp(op, left, right);
    }

    if (value instanceof AbstractBinopExpr) {
      AbstractBinopExpr binExpr = (AbstractBinopExpr) value;
      SymExpr left = valueToSymExpr(binExpr.getOp1());
      SymExpr right = valueToSymExpr(binExpr.getOp2());
      BinOp op = jimpleBinopToBinOp(binExpr);
      return new SymBinOp(op, left, right);
    }

    // Fallback: treat as symbolic variable
    return new SymVar(value.toString());
  }

  /** Get incoming DDG edges for a node */
  private static List<DataDependencyEdge> getIncomingDDGEdges(
      final WITUpNode node, final WITUpGraph graph) {
    List<DataDependencyEdge> result = new ArrayList<>();
    for (WITUpEdge edge : graph.incomingEdgesOf(node)) {
      if (edge instanceof DataDependencyEdge) {
        result.add((DataDependencyEdge) edge);
      }
    }
    return result;
  }
}
