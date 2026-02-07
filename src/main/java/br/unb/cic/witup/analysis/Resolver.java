package br.unb.cic.witup.analysis;

import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.SimpleNode;
import br.unb.cic.witup.graph.node.WITUpNode;

import java.util.*;

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
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JFieldRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.PrimitiveType.BooleanType;


public final class Resolver {
  private final WITUpGraph ddg;
  private final Map<String, SymKind> symbolTable = new HashMap<>();

  public Resolver(final WITUpGraph ddg) {
    this.ddg = ddg;
  }

  public Map<String, SymKind> getSymbolTable() {
    return symbolTable;
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

  private static SymExpr stripBooleanEncoding(SymExpr expr) {
    if (!(expr instanceof SymBinOp bin)) return expr;

    SymExpr left = bin.getLeft();
    SymExpr right = bin.getRight();

    if (right instanceof SymConst c &&
        Integer.valueOf(0).equals(c.getValue()) &&
            left.kind() == SymKind.BOOLEAN) {

      return left;
    }

    return expr;
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
    resolved = stripBooleanEncoding(resolved);
    collectSymbolKinds(resolved, this.symbolTable);

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

  private static Map<String, SymKind> findSymbolKinds(final SymExpr expr) {
    Map<String, SymKind> symbolTypes = new HashMap<>();
    collectSymbolKinds(expr, symbolTypes);
    return symbolTypes;
  }

  private static void collectSymbolKinds(SymExpr expr, Map<String, SymKind> symbolTypes) {
    if (expr instanceof SymVar v) {
      symbolTypes.put(v.getName(), v.kind());
    } else if (expr instanceof SymBinOp bin) {
      collectSymbolKinds(bin.getLeft(), symbolTypes);
      collectSymbolKinds(bin.getRight(), symbolTypes);
    } else if (expr instanceof SymField f) {
      collectSymbolKinds(f.getBase(), symbolTypes);
    } else if (expr instanceof SymVirtualInvoke inv) {
      collectSymbolKinds(inv.getBase(), symbolTypes);
      symbolTypes.put(inv.toString(), inv.kind());
    }
  }

  /** Get variable name from a Value (for assignment LHS) */
  private static String getVariableName(final Value value) {
    return value.toString();
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

  /** Resolve variables by traversing backward through DDG
   * recursely substitue until there are no more variables to resolve
   *  */
  // it's ok to reassign current in a recursive function
  private static SymExpr resolveVariables(
      SymExpr symExpr, // SUPPRESS CHECKSTYLE FinalParameters
      final Set<String> varsToResolve,
      final WITUpNode currentNode,
      final WITUpGraph graph,
      final Set<WITUpNode> visited) {
    if (varsToResolve.isEmpty()) {
      return symExpr;
    }

    if (visited.contains(currentNode)) {
      return symExpr;
    }
    visited.add(currentNode);

    List<DataDependencyEdge> incomingDDG = getIncomingDDGEdges(currentNode, graph);
    for (DataDependencyEdge edge : incomingDDG) {
      WITUpNode sourceNode = graph.getEdgeSource(edge);

      if (sourceNode instanceof SimpleNode simpleNode) {
        PropertyGraphNode propNode = simpleNode.getNode();

        if (!(propNode instanceof StmtGraphNode)) {
          continue;
        }

        StmtGraphNode stmtNode = (StmtGraphNode) propNode;
        Stmt stmt = stmtNode.getStmt();

        if (stmt instanceof JAssignStmt) {
          JAssignStmt assign = (JAssignStmt) stmt;
          Value leftOp = assign.getLeftOp();
          // local variable on the lhs e.g. $stack1 == 0
          String definedVar = getVariableName(leftOp);

          if (varsToResolve.contains(definedVar)) {
            // translate the RHS to symbolic expression
            SymExpr rhsSymExpr = valueToSymExpr(assign.getRightOp());

            // substitute this variable in our current expression
            symExpr = symExpr.substitute(definedVar, rhsSymExpr);

            varsToResolve.remove(definedVar);
            varsToResolve.addAll(findVariables(rhsSymExpr));
            symExpr = resolveVariables(symExpr, varsToResolve, sourceNode, graph, visited);
          }
        }
      }
    }

    return symExpr;
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
      return new SymStringConst(((StringConstant) value).getValue());
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

    if (value instanceof JVirtualInvokeExpr) {
      JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) value;
      SymExpr base = valueToSymExpr(invokeExpr.getBase());
      String invokedMethodName = invokeExpr.getMethodSignature().getSubSignature().getName();
      boolean returnsBoolean =
              invokeExpr.getMethodSignature().getSubSignature().getType()
                      instanceof BooleanType;

      return new SymVirtualInvoke(base, invokedMethodName, returnsBoolean);
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
