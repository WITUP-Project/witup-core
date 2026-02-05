package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.BinOp;
import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymBinOp;
import br.unb.cic.witup.analysis.SymConst;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.SymField;
import br.unb.cic.witup.analysis.SymVar;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

/**
 * Applies parameter binding over resolved conditions (SymExpr) using SootUp Value mappings.
 *
 * No GlobalExpathComposer changes required.
 */
public final class CallArgumentBindingApplier {

    public List<List<ResolvedThrowCondition>> apply(
            final List<List<ResolvedThrowCondition>> globals, final List<CallArgumentBinding> bindings) {

        Objects.requireNonNull(globals, "globals");
        Objects.requireNonNull(bindings, "bindings");

        List<List<ResolvedThrowCondition>> out = new ArrayList<>(globals.size());

        for (List<ResolvedThrowCondition> path : globals) {
            List<ResolvedThrowCondition> boundPath = new ArrayList<>(path.size());

            for (ResolvedThrowCondition c : path) {
                SymExpr node = c.getNode();

                for (CallArgumentBinding b : bindings) {
                    node = applySingleBinding(node, b.formalParamToActualArg());
                }

                boundPath.add(new ResolvedThrowCondition(node, c.isTruthValue()));
            }

            out.add(boundPath);
        }

        return out;
    }

    private static SymExpr applySingleBinding(final SymExpr expr, final Map<String, Value> binding) {
        SymExpr out = expr;
        for (Map.Entry<String, Value> e : binding.entrySet()) {
            SymExpr replacement = valueToSymExpr(e.getValue());
            out = out.substitute(e.getKey(), replacement);
        }
        return out;
    }

    private static SymExpr valueToSymExpr(final Value value) {
        if (value instanceof Local) {
            return new SymVar(value.toString());
        }

        if (value instanceof IntConstant c) {
            return new SymConst(c.getValue());
        }
        if (value instanceof DoubleConstant c) {
            return new SymConst(c.getValue());
        }
        if (value instanceof FloatConstant c) {
            return new SymConst(c.getValue());
        }
        if (value instanceof LongConstant c) {
            return new SymConst(c.getValue());
        }
        if (value instanceof StringConstant c) {
            return new SymConst(c.getValue());
        }
        if (value instanceof NullConstant) {
            return new SymConst(null);
        }

        if (value instanceof JFieldRef fieldRef) {
            if (fieldRef instanceof JInstanceFieldRef instField) {
                SymExpr base = valueToSymExpr(instField.getBase());
                return new SymField(base, instField.getFieldSignature().getName());
            }
            return new SymVar(fieldRef.toString());
        }

        if (value instanceof AbstractConditionExpr condExpr) {
            SymExpr left = valueToSymExpr(condExpr.getOp1());
            SymExpr right = valueToSymExpr(condExpr.getOp2());
            return new SymBinOp(conditionOp(condExpr), left, right);
        }

        if (value instanceof AbstractBinopExpr binExpr) {
            SymExpr left = valueToSymExpr(binExpr.getOp1());
            SymExpr right = valueToSymExpr(binExpr.getOp2());
            return new SymBinOp(binOp(binExpr), left, right);
        }

        return new SymVar(value.toString());
    }

    private static BinOp conditionOp(final AbstractConditionExpr expr) {
        if (expr instanceof JEqExpr) return BinOp.EQ;
        if (expr instanceof JNeExpr) return BinOp.NE;
        if (expr instanceof JLtExpr) return BinOp.LT;
        if (expr instanceof JLeExpr) return BinOp.LE;
        if (expr instanceof JGtExpr) return BinOp.GT;
        if (expr instanceof JGeExpr) return BinOp.GE;
        throw new IllegalArgumentException("Unknown condition expr: " + expr.getClass());
    }

    private static BinOp binOp(final AbstractBinopExpr expr) {
        if (expr instanceof JAddExpr) return BinOp.ADD;
        if (expr instanceof JSubExpr) return BinOp.SUB;
        if (expr instanceof JMulExpr) return BinOp.MUL;
        if (expr instanceof JDivExpr) return BinOp.DIV;
        if (expr instanceof JRemExpr) return BinOp.MOD;
        if (expr instanceof JCmpExpr) return BinOp.CMP;
        if (expr instanceof JCmpgExpr) return BinOp.CMPG;
        if (expr instanceof JCmplExpr) return BinOp.CMPL;
        throw new IllegalArgumentException("Unknown binop expr: " + expr.getClass());
    }
}

