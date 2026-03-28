package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.IfStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;

/**
 * Performs abstract interpretation with the interval domain over a single natural loop to produce a
 * {@link LoopSummary}.
 */
public final class LoopAbstractInterpreter {

  private static final Logger log = LoggerFactory.getLogger("LoopAbstractInterpreter");
  private static final int MAX_ITERATIONS = 5;
  public static final int MAX_UNROLLING = 3;

  private LoopAbstractInterpreter() {}

  /** Analyse a single loop and return its summary. */
  public static LoopSummary analyse(final NaturalLoop loop, final WITUpGraph graph) {
    Map<String, AssignInfo> assignments = findAssignments(loop);
    Map<String, Interval> state = initialState(assignments, loop, graph);
    Map<String, InductionInfo> inductionVars = recogniseInductionVars(assignments, loop, graph);

    // fixpoint iteration with widening
    for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
      Map<String, Interval> next = applyTransfer(state, assignments);
      Map<String, Interval> widened = new HashMap<>(state.size());
      boolean changed = false;
      for (Map.Entry<String, Interval> entry : next.entrySet()) {
        String var = entry.getKey();
        Interval prev = state.getOrDefault(var, Interval.BOTTOM);
        Interval curr = entry.getValue();
        Interval w = iter >= 2 ? Interval.widen(prev, curr) : Interval.join(prev, curr);
        widened.put(var, w);
        if (!w.equals(prev)) {
          changed = true;
        }
      }
      state = widened;
      if (!changed) {
        log.debug("Loop fixpoint converged after {} iterations", iter + 1);
        break;
      }
    }

    Map<String, AccumulationInfo> accumulations =
        recogniseAccumulations(assignments, inductionVars, loop, graph);

    log.debug(
        "recogniseAccumulations: found {} accumulations: {}",
        accumulations.size(),
        accumulations.keySet());

    return new LoopSummary(loop, state, inductionVars, accumulations);
  }

  // ── Assignment scanning ──

  private record AssignInfo(String varName, Value rhs, WITUpNode node) {}

  private static Map<String, AssignInfo> findAssignments(final NaturalLoop loop) {
    Map<String, AssignInfo> assignments = new HashMap<>();
    for (WITUpNode node : loop.body()) {
      if (!(node.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      String lhs = assign.getLeftOp().toString();
      // only track loop-carried variables: those whose RHS directly uses the
      // same Local as the LHS (e.g. sum = sum + arr[i], i = i + 1).
      // Variables like $stack5 = arr[i] are NOT loop-carried — they get a
      // fresh value each iteration and should be traced normally by the DDG.
      if (rhsUsesLocal(assign.getRightOp(), lhs)) {
        assignments.put(lhs, new AssignInfo(lhs, assign.getRightOp(), node));
      }
    }
    return assignments;
  }

  /** Check if a Jimple value directly uses a Local with the given name as an operand. */
  private static boolean rhsUsesLocal(final Value rhs, final String localName) {
    if (rhs instanceof Local l) {
      return l.toString().equals(localName);
    }
    if (rhs instanceof AbstractBinopExpr binop) {
      return isLocal(binop.getOp1(), localName) || isLocal(binop.getOp2(), localName);
    }
    return false;
  }

  private static boolean isLocal(final Value v, final String name) {
    return v instanceof Local l && l.toString().equals(name);
  }

  // ── Initial state from pre-loop definitions ──

  private static Map<String, Interval> initialState(
      final Map<String, AssignInfo> assignments, final NaturalLoop loop, final WITUpGraph graph) {
    Map<String, Interval> state = new HashMap<>();
    Set<WITUpNode> body = loop.body();

    for (Map.Entry<String, AssignInfo> entry : assignments.entrySet()) {
      String varName = entry.getKey();
      // trace DDG edges into the loop to find pre-loop definitions
      Interval init = findPreLoopValue(varName, body, graph);
      state.put(varName, init);
    }
    return state;
  }

  private static Interval findPreLoopValue(
      final String varName, final Set<WITUpNode> body, final WITUpGraph graph) {
    // look for DDG edges into any body node whose source is outside the loop
    for (WITUpNode bodyNode : body) {
      for (DataDependencyEdge ddg : graph.getIncomingDDGEdges(bodyNode)) {
        WITUpNode source = graph.getEdgeSource(ddg);
        if (body.contains(source)) {
          continue;
        }
        if (!(source.getNode() instanceof StmtGraphNode stmtNode)) {
          continue;
        }
        if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
          continue;
        }
        if (!assign.getLeftOp().toString().equals(varName)) {
          continue;
        }
        // found a pre-loop definition
        Value rhs = assign.getRightOp();
        if (rhs instanceof IntConstant c) {
          return Interval.constant(c.getValue());
        }
        return Interval.TOP;
      }
    }
    return Interval.TOP;
  }

  // ── Transfer functions ──

  private static Map<String, Interval> applyTransfer(
      final Map<String, Interval> state, final Map<String, AssignInfo> assignments) {
    Map<String, Interval> next = new HashMap<>(state);
    for (Map.Entry<String, AssignInfo> entry : assignments.entrySet()) {
      String var = entry.getKey();
      Value rhs = entry.getValue().rhs();
      next.put(var, transferValue(rhs, state));
    }
    return next;
  }

  private static Interval transferValue(final Value rhs, final Map<String, Interval> state) {
    if (rhs instanceof IntConstant c) {
      return Interval.constant(c.getValue());
    }
    if (rhs instanceof Local l) {
      return state.getOrDefault(l.toString(), Interval.TOP);
    }
    if (rhs instanceof JAddExpr add) {
      return Interval.add(
          operandInterval(add.getOp1(), state), operandInterval(add.getOp2(), state));
    }
    if (rhs instanceof JSubExpr sub) {
      return Interval.sub(
          operandInterval(sub.getOp1(), state), operandInterval(sub.getOp2(), state));
    }
    if (rhs instanceof JMulExpr mul) {
      return Interval.mul(
          operandInterval(mul.getOp1(), state), operandInterval(mul.getOp2(), state));
    }
    if (rhs instanceof JArrayRef) {
      return Interval.TOP;
    }
    // unrecognised expression (invoke, cast, etc.)
    return Interval.TOP;
  }

  private static Interval operandInterval(final Value v, final Map<String, Interval> state) {
    if (v instanceof IntConstant c) {
      return Interval.constant(c.getValue());
    }
    if (v instanceof Local l) {
      return state.getOrDefault(l.toString(), Interval.TOP);
    }
    return Interval.TOP;
  }

  // ── Induction variable recognition ──

  private static Map<String, InductionInfo> recogniseInductionVars(
      final Map<String, AssignInfo> assignments, final NaturalLoop loop, final WITUpGraph graph) {
    Map<String, InductionInfo> result = new HashMap<>();

    for (Map.Entry<String, AssignInfo> entry : assignments.entrySet()) {
      String varName = entry.getKey();
      Value rhs = entry.getValue().rhs();

      // pattern: var = var + const  or  var = var - const
      long step = extractStep(varName, rhs);
      if (step == 0) {
        continue;
      }

      // find the loop exit condition involving this variable
      BoundInfo bound = findLoopBound(varName, loop);
      log.debug("inductionVar check: var={} step={} bound={} headerType={}",
              varName, step, bound, loop.header().getClass().getSimpleName());
      if (bound == null) {
        continue;
      }

      // find pre-loop initial value as a SymExpr
      SymExpr initExpr = findPreLoopSymExpr(varName, loop.body(), graph);
      if (initExpr == null) {
        continue;
      }

      // resolve bound expression locals (e.g. $stack4 → arr.length)
      SymExpr resolvedBound = resolvePreLoopLocals(bound.boundExpr, loop.body(), graph);

      result.put(
          varName, new InductionInfo(varName, initExpr, resolvedBound, step, bound.comparison));
      log.debug(
          "Recognised induction variable: {} init={} bound={} step={}",
          varName,
          initExpr,
          resolvedBound,
          step);
    }
    return result;
  }

  /**
   * Check if rhs is {@code varName + const} or {@code varName - const}. Returns the step (positive
   * for add, negative for sub), or 0 if not an induction pattern.
   */
  private static long extractStep(final String varName, final Value rhs) {
    if (!(rhs instanceof AbstractBinopExpr binop)) {
      return 0;
    }
    boolean isAdd = rhs instanceof JAddExpr;
    boolean isSub = rhs instanceof JSubExpr;
    if (!isAdd && !isSub) {
      return 0;
    }

    Value op1 = binop.getOp1();
    Value op2 = binop.getOp2();

    // var + const
    if (op1 instanceof Local l && l.toString().equals(varName) && op2 instanceof IntConstant c) {
      return isAdd ? c.getValue() : -c.getValue();
    }
    // const + var
    if (op2 instanceof Local l && l.toString().equals(varName) && op1 instanceof IntConstant c) {
      return isAdd ? c.getValue() : -c.getValue();
    }
    return 0;
  }

  private record BoundInfo(SymExpr boundExpr, BinOp comparison) {}

  /**
   * Find the loop exit condition for the given induction variable. Scans the exit edges for a
   * BooleanCFGEdge whose source node has a JIfStmt condition referencing the variable.
   */
  private static BoundInfo findLoopBound(final String varName, final NaturalLoop loop) {
    for (WITUpEdge edge : loop.exitEdges()) {
      if (!(edge instanceof BooleanCFGEdge boolEdge)) {
        continue;
      }
      WITUpNode condNode = edge.getSource();

      // extract condition from whatever node type wraps the if-statement
      AbstractConditionExpr cond = null;
      if (condNode instanceof IfStatementNode ifNode) {
        cond = ifNode.getCondition();
      } else if (condNode.getNode() instanceof StmtGraphNode sn
          && sn.getStmt() instanceof JIfStmt ifStmt) {
        cond = ifStmt.getCondition();
      }
      if (cond == null) {
        continue;
      }

      boolean exitOnTrue = boolEdge.getCondition();
      Value lhs = cond.getOp1();
      Value rhs = cond.getOp2();

      // check if the induction var is on the left: "if (i >= bound) goto exit"
      if (lhs instanceof Local l && l.toString().equals(varName)) {
        BinOp jimpleOp = conditionToBinOp(cond);
        if (jimpleOp == null) {
          continue;
        }
        BinOp loopOp = exitOnTrue ? negate(jimpleOp) : jimpleOp;
        return new BoundInfo(SymExpr.fromJimple(rhs), loopOp);
      }

      // check if the induction var is on the right: "if (bound <= i) goto exit"
      if (rhs instanceof Local l && l.toString().equals(varName)) {
        BinOp jimpleOp = conditionToBinOp(cond);
        if (jimpleOp == null) {
          continue;
        }
        BinOp flipped = flip(jimpleOp);
        BinOp loopOp = exitOnTrue ? negate(flipped) : flipped;
        return new BoundInfo(SymExpr.fromJimple(lhs), loopOp);
      }
    }
    return null;
  }

  private static SymExpr findPreLoopSymExpr(
      final String varName, final Set<WITUpNode> body, final WITUpGraph graph) {
    for (WITUpNode bodyNode : body) {
      for (DataDependencyEdge ddg : graph.getIncomingDDGEdges(bodyNode)) {
        WITUpNode source = graph.getEdgeSource(ddg);
        if (body.contains(source)) {
          continue;
        }
        if (!(source.getNode() instanceof StmtGraphNode stmtNode)) {
          continue;
        }
        if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
          continue;
        }
        if (!assign.getLeftOp().toString().equals(varName)) {
          continue;
        }
        return SymExpr.fromJimple(assign.getRightOp());
      }
    }
    return null;
  }

  // ── Accumulation recognition ──

  private static Map<String, AccumulationInfo> recogniseAccumulations(
      final Map<String, AssignInfo> assignments,
      final Map<String, InductionInfo> inductionVars,
      final NaturalLoop loop,
      final WITUpGraph graph) {
    Map<String, AccumulationInfo> result = new HashMap<>();

    for (Map.Entry<String, AssignInfo> entry : assignments.entrySet()) {
      String varName = entry.getKey();
      if (inductionVars.containsKey(varName)) {
        continue;
      }
      Value rhs = entry.getValue().rhs();
      if (!(rhs instanceof AbstractBinopExpr binop)) {
        continue;
      }
      BinOp op = SymExpr.fromJimpleBinop(binop);
      if (op != BinOp.ADD && op != BinOp.SUB && op != BinOp.MUL) {
        continue;
      }

      // extract the non-self operand
      Value nonSelf = extractNonSelfOperand(binop, varName);
      if (nonSelf == null) {
        continue;
      }

      // resolve stack variables within the loop body, then resolve locals to pre-loop defs
      // (skip induction var names so they remain as SymVar for pattern matching later)
      Value resolved = resolveInLoopBody(nonSelf, loop, MAX_UNROLLING);
      SymExpr stepExpr = SymExpr.fromJimple(resolved);
      stepExpr = resolvePreLoopLocals(stepExpr, loop.body(), graph, inductionVars.keySet());

      // check if the step expression references any induction variable
      Set<String> stepVars = new HashSet<>();
      stepExpr.collectVarNames(stepVars);
      String indVar = null;
      for (String ivName : inductionVars.keySet()) {
        if (stepVars.contains(ivName)) {
          indVar = ivName;
          break;
        }
      }

      SymExpr initExpr = findPreLoopSymExpr(varName, loop.body(), graph);
      if (initExpr == null) {
        continue;
      }

      result.put(varName, new AccumulationInfo(varName, op, stepExpr, initExpr, indVar));
      log.debug(
          "Recognised accumulation: {} = {} {} {} (inductionVar={})",
          varName,
          varName,
          op,
          stepExpr,
          indVar);
    }
    return result;
  }

  private static Value extractNonSelfOperand(final AbstractBinopExpr binop, final String varName) {
    Value op1 = binop.getOp1();
    Value op2 = binop.getOp2();
    if (isLocal(op1, varName)) {
      return op2;
    }
    if (isLocal(op2, varName)) {
      return op1;
    }
    return null;
  }

  /** Resolve stack variables within the loop body (mini backward-substitute). */
  private static Value resolveInLoopBody(
      final Value value, final NaturalLoop loop, final int maxDepth) {
    if (maxDepth <= 0) {
      return value;
    }
    if (!(value instanceof Local l)) {
      return value;
    }
    if (!l.toString().contains("$stack")) {
      return value;
    }
    String name = l.toString();
    for (WITUpNode node : loop.body()) {
      if (!(node.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      if (!assign.getLeftOp().toString().equals(name)) {
        continue;
      }
      return resolveInLoopBody(assign.getRightOp(), loop, maxDepth - 1);
    }
    return value;
  }

  /**
   * Resolve free locals in a SymExpr to their pre-loop definitions (including identity stmts for
   * parameters). This ensures that e.g. SymVar("arr") becomes SymParamRef(0, int[]).
   */
  private static SymExpr resolvePreLoopLocals(
      final SymExpr expr, final Set<WITUpNode> body, final WITUpGraph graph) {
    return resolvePreLoopLocals(expr, body, graph, Set.of());
  }

  private static SymExpr resolvePreLoopLocals(
      final SymExpr expr,
      final Set<WITUpNode> body,
      final WITUpGraph graph,
      final Set<String> skip) {
    Set<String> vars = new HashSet<>();
    expr.collectVarNames(vars);
    vars.removeAll(skip);
    if (vars.isEmpty()) {
      return expr;
    }

    SymExpr result = expr;
    for (String varName : vars) {
      SymExpr resolved = findPreLoopDefinition(varName, body, graph);
      if (resolved != null && !(resolved instanceof SymParamRef)) {
        // keep SymVar for param-backed locals (e.g. arr) — Z3 resolves them via DDG
        result = substituteInExpr(result, varName, resolved);
      }
    }
    return result;
  }

  /**
   * Find a pre-loop definition for a variable, including JIdentityStmt (parameters) and
   * JAssignStmt.
   */
  private static SymExpr findPreLoopDefinition(
      final String varName, final Set<WITUpNode> body, final WITUpGraph graph) {
    for (WITUpNode bodyNode : body) {
      for (DataDependencyEdge ddg : graph.getIncomingDDGEdges(bodyNode)) {
        WITUpNode source = graph.getEdgeSource(ddg);
        if (body.contains(source)) {
          continue;
        }
        if (!(source.getNode() instanceof StmtGraphNode stmtNode)) {
          continue;
        }
        if (stmtNode.getStmt() instanceof JAssignStmt assign
            && assign.getLeftOp().toString().equals(varName)) {
          return SymExpr.fromJimple(assign.getRightOp());
        }
        if (stmtNode.getStmt() instanceof JIdentityStmt identity
            && identity.getLeftOp().toString().equals(varName)) {
          return SymExpr.fromJimple(identity.getRightOp());
        }
      }
    }
    return null;
  }

  /** Substitute a variable in a SymExpr, handling SymArrayRef which doesn't recurse. */
  private static SymExpr substituteInExpr(
      final SymExpr expr, final String varName, final SymExpr replacement) {
    if (expr instanceof SymArrayRef ref) {
      SymExpr newArray = substituteInExpr(ref.getArray(), varName, replacement);
      SymExpr newIndex = substituteInExpr(ref.getIndex(), varName, replacement);
      if (newArray != ref.getArray() || newIndex != ref.getIndex()) {
        return new SymArrayRef(newArray, newIndex);
      }
      return expr;
    }
    return expr.substitute(varName, replacement);
  }

  private static BinOp conditionToBinOp(final AbstractConditionExpr cond) {
    if (cond instanceof JLtExpr) {
      return BinOp.LT;
    }
    if (cond instanceof JLeExpr) {
      return BinOp.LE;
    }
    if (cond instanceof JGtExpr) {
      return BinOp.GT;
    }
    if (cond instanceof JGeExpr) {
      return BinOp.GE;
    }
    return null;
  }

  private static BinOp negate(final BinOp op) {
    return switch (op) {
      case LT -> BinOp.GE;
      case LE -> BinOp.GT;
      case GT -> BinOp.LE;
      case GE -> BinOp.LT;
      default -> op;
    };
  }

  private static BinOp flip(final BinOp op) {
    return switch (op) {
      case LT -> BinOp.GT;
      case LE -> BinOp.GE;
      case GT -> BinOp.LT;
      case GE -> BinOp.LE;
      default -> op;
    };
  }
}
