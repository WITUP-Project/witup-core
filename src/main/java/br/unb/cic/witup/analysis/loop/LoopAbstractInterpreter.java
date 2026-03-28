package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.IfStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.HashMap;
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

/**
 * Performs abstract interpretation with the interval domain over a single natural loop to produce a
 * {@link LoopSummary}.
 */
public final class LoopAbstractInterpreter {

  private static final Logger log = LoggerFactory.getLogger("LoopAbstractInterpreter");
  private static final int MAX_ITERATIONS = 5;

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

    return new LoopSummary(loop, state, inductionVars);
  }

  // ── Assignment scanning ──

  private record AssignInfo(String varName, Value rhs, WITUpNode node) {}

  private static Map<String, AssignInfo> findAssignments(
      final NaturalLoop loop) {
    Map<String, AssignInfo> assignments = new HashMap<>();
    for (WITUpNode node : loop.body()) {
      if (!(node.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      String lhs = assign.getLeftOp().toString();
      assignments.put(lhs, new AssignInfo(lhs, assign.getRightOp(), node));
    }
    return assignments;
  }

  // ── Initial state from pre-loop definitions ──

  private static Map<String, Interval> initialState(
      final Map<String, AssignInfo> assignments,
      final NaturalLoop loop,
      final WITUpGraph graph) {
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
      final Map<String, AssignInfo> assignments,
      final NaturalLoop loop,
      final WITUpGraph graph) {
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
      if (bound == null) {
        continue;
      }

      // find pre-loop initial value as a SymExpr
      SymExpr initExpr = findPreLoopSymExpr(varName, loop.body(), graph);
      if (initExpr == null) {
        continue;
      }

      result.put(
          varName,
          new InductionInfo(varName, initExpr, bound.boundExpr, step, bound.comparison));
      log.debug(
          "Recognised induction variable: {} init={} bound={} step={}",
          varName,
          initExpr,
          bound.boundExpr,
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
    if (op1 instanceof Local l
        && l.toString().equals(varName)
        && op2 instanceof IntConstant c) {
      return isAdd ? c.getValue() : -c.getValue();
    }
    // const + var
    if (op2 instanceof Local l
        && l.toString().equals(varName)
        && op1 instanceof IntConstant c) {
      return isAdd ? c.getValue() : -c.getValue();
    }
    return 0;
  }

  private record BoundInfo(SymExpr boundExpr, BinOp comparison) {}

  /**
   * Find the loop exit condition for the given induction variable. Looks at the loop header's
   * IfStatementNode and the BooleanCFGEdge that exits the loop.
   */
  private static BoundInfo findLoopBound(
      final String varName, final NaturalLoop loop) {
    if (!(loop.header() instanceof IfStatementNode ifNode)) {
      return null;
    }
    AbstractConditionExpr cond = ifNode.getCondition();
    Value lhs = cond.getOp1();
    Value rhs = cond.getOp2();

    // determine which BooleanCFGEdge exits the loop
    boolean exitOnTrue = false;
    for (WITUpEdge edge : loop.exitEdges()) {
      if (edge instanceof BooleanCFGEdge boolEdge && edge.getSource().equals(loop.header())) {
        exitOnTrue = boolEdge.getCondition();
        break;
      }
    }

    // the condition as written in Jimple is: "if (lhs op rhs) goto exitTarget"
    // when exitOnTrue, the loop CONTINUES when the condition is false
    // when exitOnFalse, the loop CONTINUES when the condition is true

    // check if the induction var is on the left: "if (i >= bound) goto exit"
    if (lhs instanceof Local l && l.toString().equals(varName)) {
      BinOp jimpleOp = conditionToBinOp(cond);
      if (jimpleOp == null) {
        return null;
      }
      // if exit on true: loop continues while NOT(condition), i.e. the negated comparison
      // gives the bound. e.g. "if (i >= len) exit" → loop runs while i < len → bound is len, LT
      BinOp loopOp = exitOnTrue ? negate(jimpleOp) : jimpleOp;
      return new BoundInfo(SymExpr.fromJimple(rhs), loopOp);
    }

    // check if the induction var is on the right: "if (bound <= i) goto exit"
    if (rhs instanceof Local l && l.toString().equals(varName)) {
      BinOp jimpleOp = conditionToBinOp(cond);
      if (jimpleOp == null) {
        return null;
      }
      BinOp flipped = flip(jimpleOp);
      BinOp loopOp = exitOnTrue ? negate(flipped) : flipped;
      return new BoundInfo(SymExpr.fromJimple(lhs), loopOp);
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
