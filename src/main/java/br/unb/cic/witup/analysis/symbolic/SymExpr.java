package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.BinOp;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
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
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JCmpExpr;
import sootup.core.jimple.common.expr.JCmpgExpr;
import sootup.core.jimple.common.expr.JCmplExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;

public abstract class SymExpr {
  public abstract <T> T accept(SymExprVisitor<T> visitor);

  public abstract SymExpr substitute(String varName, SymExpr replacement);

  /***
   * Ignored for most types, useful for SymParam
   *
   * @param idx index of the param
   * @param actual index to compare
   * @return new index
   */
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    return this; // default: no substitution
  }

  public abstract String toString();

  public abstract boolean contains(String varName);

  public abstract SymKind kind();

  public static SymExpr fromValue(final Value value) {
    return switch (value) {
      case Local l when l.getType() instanceof ArrayType at ->
          new SymArray(l.toString(), symKindFromType(at.getElementType()), l.getType().toString());

      case Local l -> new SymVar(l.toString(), symKindFromType(l.getType()));
      case IntConstant c -> new SymConst(c.getValue());
      case DoubleConstant c -> new SymConst(c.getValue());
      case FloatConstant c -> new SymConst(c.getValue());
      case LongConstant c -> new SymConst(c.getValue());
      case StringConstant c -> new SymStringConst(c.getValue());
      case NullConstant ignored -> new SymConst(null); // Consider if we need SymNull
      case JInstanceFieldRef r -> fromFieldRef(r);
      case AbstractConditionExpr e -> fromAbstractCondExpr(e);
      case AbstractBinopExpr e -> fromAbstractBinOpExpr(e);
      case JVirtualInvokeExpr e -> fromVirtualInvokeExpr(e);
      case JArrayRef r -> fromArrayRef(r);
      case JLengthExpr e -> fromLength(e);
      case JNewArrayExpr e -> fromNewArray(e);
      case JCastExpr e -> fromCast(e);
      case JInstanceOfExpr e -> fromInstanceOf(e);
      case JParameterRef r -> fromParamRef(r);
      default -> new SymVar(value.toString(), symKindFromType(value.getType()));
    };
  }

  public static SymKind symKindFromType(final Type type) {
    if (type instanceof ArrayType at) {
      return symKindFromType(at.getElementType());
    }

    return switch (type) {
      case PrimitiveType.ShortType ignored -> SymKind.INT;
      case PrimitiveType.ByteType ignored -> SymKind.INT;
      case PrimitiveType.BooleanType ignored -> SymKind.BOOLEAN;
      case PrimitiveType.IntType ignored -> SymKind.INT;
      case PrimitiveType.LongType ignored -> SymKind.INT;
      case PrimitiveType.FloatType ignored -> SymKind.REAL;
      case PrimitiveType.DoubleType ignored -> SymKind.REAL;
      case ClassType ct when ct.getFullyQualifiedName().equals("java.lang.String") ->
          SymKind.STRING;
      case ClassType ignore -> SymKind.OBJECT;
      default -> SymKind.OTHER;
    };
  }

  private static SymExpr fromFieldRef(final JInstanceFieldRef r) {
    // this.<ClassName: type fieldName>
    SymExpr base = fromValue(r.getBase());
    String fieldName = r.getFieldSignature().getName();
    return new SymFieldAccess(base, fieldName, symKindFromType(r.getType()));
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

  private static SymExpr fromArrayRef(final JArrayRef r) {
    SymArray base = (SymArray) fromValue(r.getBase());
    SymExpr indexExpr = fromValue(r.getIndex());
    return new SymArrayRef(base, indexExpr);
  }

  private static SymExpr fromLength(final JLengthExpr r) {
    SymExpr op = fromValue(r.getOp());
    return new SymLength(op);
  }

  private static SymExpr fromNewArray(final JNewArrayExpr r) {
    String name = r.toString();
    return new SymArray(name, symKindFromType(r.getType()), r.getType().toString());
  }

  private static SymExpr fromCast(final JCastExpr r) {
    SymExpr op = fromValue(r.getOp());
    String type = r.getType().toString();
    return new SymCast(op, type);
  }

  private static SymExpr fromInstanceOf(final JInstanceOfExpr r) {
    SymExpr op = fromValue(r.getOp());
    String type = r.getCheckType().toString();
    return new SymInstanceOf(op, type);
  }

  private static SymExpr fromParamRef(final JParameterRef r) {
    int index = r.getIndex();
    SymKind kind = symKindFromType(r.getType());
    return new SymParam(index, kind);
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
    if (left instanceof SymBinOp leftBinOp
        && right instanceof SymConst rightConst
        && (leftBinOp.getOp() == BinOp.CMPG
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

    SymExpr lhs = bin.getLeft();
    SymExpr rhs = bin.getRight();

    // when we have a Jimple comparison whose stack variable traces back to
    // a method call, we don't need the equality; only the respective symbol
    // and the truth value
    if (rhs instanceof SymConst c
            && Integer.valueOf(0).equals(c.getValue())
            && (lhs.kind() == SymKind.BOOLEAN_METHOD)
        || lhs.kind() == SymKind.BOOLEAN) {

      return lhs;
    }

    return expr;
  }
}
