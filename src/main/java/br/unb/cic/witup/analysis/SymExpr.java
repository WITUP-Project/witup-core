package br.unb.cic.witup.analysis;

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
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.types.PrimitiveType;

public abstract class SymExpr {
  public abstract <T> T accept(SymExprVisitor<T> visitor);

  public abstract SymExpr substitute(String varName, SymExpr replacement);

  public abstract String toString();

  public abstract boolean contains(String varName);

  // not sure if this is good practice
  public abstract SymKind kind();

  public static SymExpr fromValue(final Value value) {
    return switch (value) {
      case Local l -> new SymVar(l.toString());
      case IntConstant c -> new SymConst(c.getValue());
      case DoubleConstant c -> new SymConst(c.getValue());
      case FloatConstant c -> new SymConst(c.getValue());
      case LongConstant c -> new SymConst(c.getValue());
      case StringConstant c -> new SymStringConst(c.getValue());
      case NullConstant ignored -> new SymConst(null);
      case JInstanceFieldRef r -> fromFieldRef(r);
      case AbstractConditionExpr e -> fromAbstractCondExpr(e);
      case AbstractBinopExpr e -> fromAbstractBinOpExpr(e);
      case JVirtualInvokeExpr e -> fromVirtualInvokeExpr(e);
      default -> new SymVar(value.toString());
    };
  }

  private static SymExpr fromFieldRef(final JInstanceFieldRef r) {
    // this.<ClassName: type fieldName>
    SymExpr base = fromValue(r.getBase());
    String fieldName = r.getFieldSignature().getName();
    return new SymField(base, fieldName);
  }

  private static SymExpr fromAbstractCondExpr(final AbstractConditionExpr e) {
    SymExpr left = fromValue(e.getOp1());
    SymExpr right = fromValue(e.getOp2());
    BinOp op = jimpleOpToBinOp(e);
    return new SymBinOp(op, left, right);
  }

  private static SymExpr fromAbstractBinOpExpr(final AbstractBinopExpr e) {
    SymExpr left = fromValue(e.getOp1());
    SymExpr right = fromValue(e.getOp2());
    BinOp op = jimpleBinopToBinOp(e);
    return new SymBinOp(op, left, right);
  }

  private static SymExpr fromVirtualInvokeExpr(final JVirtualInvokeExpr e) {
    SymExpr base = fromValue(e.getBase());
    String invokedMethodName = e.getMethodSignature().getSubSignature().getName();
    boolean returnsBoolean =
        e.getMethodSignature().getSubSignature().getType() instanceof PrimitiveType.BooleanType;

    return new SymVirtualInvoke(base, invokedMethodName, returnsBoolean);
  }

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

  // Simplify patterns like (x cmpg y) >= 0 to x >= y
  public static SymExpr simplifyCmpPatterns(final SymExpr expr) {
    if (!(expr instanceof SymBinOp binOp)) {
      return expr;
    }

    SymExpr left = simplifyCmpPatterns(binOp.getLeft());
    SymExpr right = simplifyCmpPatterns(binOp.getRight());

    // Pattern: (x cmpg/cmpl y) op 0
    if (left instanceof SymBinOp leftBinOp && right instanceof SymConst rightConst) {

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

  public static SymExpr stripBooleanEncoding(final SymExpr expr) {
    if (!(expr instanceof SymBinOp bin)) {
      return expr;
    }

    SymExpr left = bin.getLeft();
    SymExpr right = bin.getRight();

    // when we have a Jimple comparison whose stack variable traces back to
    // a method call, we don't need the equality; only the respective symbol
    // and the truth value
    if (right instanceof SymConst c
        && Integer.valueOf(0).equals(c.getValue())
        && left.kind() == SymKind.BOOLEAN_METHOD) {

      return left;
    }

    return expr;
  }
}
