package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.ClassConstant;
import sootup.core.jimple.common.constant.DoubleConstant;
import sootup.core.jimple.common.constant.FloatConstant;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.LongConstant;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractBinopExpr;
import sootup.core.jimple.common.expr.JAddExpr;
import sootup.core.jimple.common.expr.JAndExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JCmpExpr;
import sootup.core.jimple.common.expr.JCmpgExpr;
import sootup.core.jimple.common.expr.JCmplExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JGeExpr;
import sootup.core.jimple.common.expr.JGtExpr;
import sootup.core.jimple.common.expr.JInstanceOfExpr;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.jimple.common.expr.JLeExpr;
import sootup.core.jimple.common.expr.JLengthExpr;
import sootup.core.jimple.common.expr.JLtExpr;
import sootup.core.jimple.common.expr.JMulExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.expr.JNegExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.expr.JOrExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JShlExpr;
import sootup.core.jimple.common.expr.JShrExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.expr.JSubExpr;
import sootup.core.jimple.common.expr.JUshrExpr;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.jimple.common.expr.JXorExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JParameterRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.ref.JThisRef;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;

public abstract class SymExpr {
  private final SymKind kind;
  public SymExpr(final SymKind kind) {
    this.kind = kind;
  }
  public final SymKind getKind() {
    return kind;
  }

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

  public static SymExpr fromJimple(final Value value) {
    return switch (value) {
      case Local l -> new SymVar(l);
      case IntConstant c -> new SymIntConst(c);
      case DoubleConstant c -> new SymDoubleConst(c);
      case FloatConstant c -> new SymFloatConst(c);
      case LongConstant c -> new SymLongConstant(c);
      case StringConstant c -> new SymStringConst(c);
      case NullConstant n -> SymNull.INSTANCE;
      case JInstanceFieldRef r -> new SymFieldAccess(fromJimple(r.getBase()), r);
      case AbstractBinopExpr e -> SymBinOp.fromBinopExpr(e);
      case JVirtualInvokeExpr e -> SymVirtualInvoke.fromVirtualInvokeExpr(e);
      case JStaticInvokeExpr e -> new SymStaticInvoke(e);
      case JInterfaceInvokeExpr e -> SymInterfaceInvoke.fromInterfaceInvokeExpr(e);
      case JSpecialInvokeExpr e -> SymSpecialInvoke.fromSpecialInvokeExpr(e);
      case JDynamicInvokeExpr e -> new SymDynamicInvoke(e);
      case JArrayRef r -> SymArrayRef.fromArrayRef(r);
      case JNewArrayExpr e -> new SymArray(e);
      case JNewMultiArrayExpr e -> new SymNewMultiArray(e);
      case JLengthExpr e -> new SymLength(e);
      case JCastExpr e -> new SymCast(e);
      case JInstanceOfExpr e -> new SymInstanceOf(e);
      case JParameterRef r -> new SymParamRef(r);
      case JThisRef r -> new SymThisRef(r);
      case JCaughtExceptionRef r -> new SymCaughtExceptionRef(r);
      case JNewExpr e -> new SymNew(e);
      case JStaticFieldRef e -> new SymStaticFieldRef(e);
      case JNegExpr e -> new SymNeg(e);
      case ClassConstant c -> new SymClassConst(c);
      default -> throw new IllegalStateException("Unexpected value: " + value);
    };
  }

  public static SymKind fromJimpleType(final Type type) {
    if (type instanceof ArrayType at) {
      return fromJimpleType(at.getElementType());
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

  static BinOp fromJimpleBinop(final AbstractBinopExpr expr) {
    return switch (expr) {
      case JEqExpr e -> BinOp.EQ;
      case JNeExpr e -> BinOp.NE;
      case JLtExpr e -> BinOp.LT;
      case JLeExpr e -> BinOp.LE;
      case JGtExpr e -> BinOp.GT;
      case JGeExpr e -> BinOp.GE;
      case JAddExpr e -> BinOp.ADD;
      case JSubExpr e -> BinOp.SUB;
      case JMulExpr e -> BinOp.MUL;
      case JDivExpr e -> BinOp.DIV;
      case JRemExpr e -> BinOp.MOD;
      case JCmpExpr e -> BinOp.CMP;
      case JCmpgExpr e -> BinOp.CMPG;
      case JCmplExpr e -> BinOp.CMPL;
      case JShrExpr e -> BinOp.SHIFT_RIGHT;
      case JUshrExpr e -> BinOp.UNSIGNED_SHIFT_RIGHT;
      case JShlExpr e -> BinOp.SHIFT_LEFT;
      case JAndExpr e -> BinOp.AND;
      case JOrExpr e -> BinOp.OR;
      case JXorExpr e -> BinOp.XOR;
      default -> throw new IllegalArgumentException("Unknown binop expr: " + expr.getClass());
    };
  }

  // Simplify patterns like (x cmpg y) >= 0 to x >= y
  public static SymExpr simplifyCmpPatterns(final SymExpr expr) {
    if (!(expr instanceof SymBinOp binOp)) {
      return expr;
    }
    SymExpr lhs = simplifyCmpPatterns(binOp.getLhs());
    SymExpr rhs = simplifyCmpPatterns(binOp.getRhs());
    // Pattern: (x cmpg/cmpl y) op 0
    if (lhs instanceof SymBinOp leftBinOp
        && isZeroConst(rhs)
        && (leftBinOp.getOp() == BinOp.CMPG
            || leftBinOp.getOp() == BinOp.CMPL
            || leftBinOp.getOp() == BinOp.CMP)) {
      // cmpg/cmpl returns: -1 if left < right, 0 if equal, 1 if left > right
      // So: (x cmpg y) >= 0 means x >= y
      //     (x cmpg y) > 0 means x > y
      //     (x cmpg y) == 0 means x == y
      //     (x cmpg y) < 0 means x < y
      //     (x cmpg y) <= 0 means x <= y
      return new SymBinOp(binOp.getOp(), leftBinOp.getLhs(), leftBinOp.getRhs());
    }
    // Return with simplified children
    if (lhs != binOp.getLhs() || rhs != binOp.getRhs()) {
      return new SymBinOp(binOp.getOp(), lhs, rhs);
    }
    return expr;
  }

  public static SymExpr stripBooleanEncoding(final SymExpr expr) {
    if (!(expr instanceof SymBinOp bin)) {
      return expr;
    }
    SymExpr lhs = bin.getLhs();
    SymExpr rhs = bin.getRhs();
    if (isZeroConst(rhs)
        && (lhs.getKind() == SymKind.BOOLEAN_METHOD || lhs.getKind() == SymKind.BOOLEAN)) {
      return lhs;
    }
    return expr;
  }

  public static SymExpr simplifyBoxingPatterns(final SymExpr expr) {
    SymExpr simplified =
        switch (expr) {
          case SymBinOp b ->
              new SymBinOp(
                  b.getOp(),
                  simplifyBoxingPatterns(b.getLhs()),
                  simplifyBoxingPatterns(b.getRhs()));
          case SymITE ite ->
              new SymITE(
                  simplifyBoxingPatterns(ite.getCondition()),
                  simplifyBoxingPatterns(ite.getThenExpr()),
                  simplifyBoxingPatterns(ite.getElseExpr()));
          default -> expr;
        };

    if (!(expr instanceof SymVirtualInvoke invoke)) {
      return simplified;
    }
    if (!(simplified instanceof SymVirtualInvoke inv)) {
      return simplified;
    }
    if (!isUnboxingCall(invoke.getSignature())) {
      return simplified;
    }
    SymExpr base = invoke.getBase();
    if (base instanceof SymCast cast) {
      SymExpr inner = cast.getOp();
      // unwrap valueOf(e) -> e
      if (inner instanceof SymStaticInvoke staticInvoke
          && isBoxingCall(staticInvoke.getInvokeName())
          && staticInvoke.getArgs().size() == 1) {
        return staticInvoke.getArgs().get(0);
      }
      return inner;
    }
    if (base instanceof SymStaticInvoke staticInvoke
        && isBoxingCall(staticInvoke.getInvokeName())
        && staticInvoke.getArgs().size() == 1) {
      return staticInvoke.getArgs().get(0);
    }
    return simplified;
  }

  private static boolean isUnboxingCall(final String name) {
    return name.equals("intValue")
        || name.equals("longValue")
        || name.equals("doubleValue")
        || name.equals("floatValue")
        || name.equals("shortValue")
        || name.equals("byteValue")
        || name.equals("booleanValue");
  }

  private static boolean isBoxingCall(final String name) {
    return name.contains("valueOf");
  }

  private static boolean isZeroConst(final SymExpr expr) {
    return switch (expr) {
      case SymConst c ->
          Integer.valueOf(0).equals(c.getValue())
              || Double.valueOf(0.0).equals(c.getValue())
              || Float.valueOf(0.0f).equals(c.getValue());
      case SymIntConst ic -> ic.getValue() == 0;
      case SymDoubleConst dc -> dc.getValue() == 0.0;
      case SymFloatConst fc -> fc.getValue() == 0.0f;
      default -> false;
    };
  }
}
