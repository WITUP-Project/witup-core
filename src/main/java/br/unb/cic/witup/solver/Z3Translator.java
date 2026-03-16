package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.BinOp;
import br.unb.cic.witup.analysis.symbolic.SymArray;
import br.unb.cic.witup.analysis.symbolic.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymCast;
import br.unb.cic.witup.analysis.symbolic.SymCaughtExceptionRef;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymDoubleConst;
import br.unb.cic.witup.analysis.symbolic.SymDynamicInvoke;
import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymFieldAccess;
import br.unb.cic.witup.analysis.symbolic.SymFloatConst;
import br.unb.cic.witup.analysis.symbolic.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.SymInterfaceInvoke;
import br.unb.cic.witup.analysis.symbolic.SymLength;
import br.unb.cic.witup.analysis.symbolic.SymLongConstant;
import br.unb.cic.witup.analysis.symbolic.SymNeg;
import br.unb.cic.witup.analysis.symbolic.SymNew;
import br.unb.cic.witup.analysis.symbolic.SymNull;
import br.unb.cic.witup.analysis.symbolic.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.SymSpecialInvoke;
import br.unb.cic.witup.analysis.symbolic.SymStaticFieldRef;
import br.unb.cic.witup.analysis.symbolic.SymStaticInvoke;
import br.unb.cic.witup.analysis.symbolic.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.SymThisRef;
import br.unb.cic.witup.analysis.symbolic.SymVar;
import br.unb.cic.witup.analysis.symbolic.SymVirtualInvoke;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Sort;
import com.microsoft.z3.UninterpretedSort;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class Z3Translator implements SymExprVisitor<Expr<?>> {
  public static final String JAVA_LANG_OBJECT = "java.lang.Object";
  public static final String AS_STR_SUFFIX = "_as_str";
  public static final String INSTANCEOF = "_instanceof_";
  public static final String FIELD_DECL_PREFIX = "field_";
  public static final String ARRAY_SELECT = "select:";
  public static final String IS_NULL = "_is_null";
  public static final String NULL_STR = "__null__";
  public static final String THIS_STR = "__this__";
  public static final int BITS_32 = 32;
  public static final String BOOL_SUFFIX = "_bool";
  public static final String INT_SUFFIX = "_int";
  private final Context context;
  private final Z3SortDetector sortInferrer;
  private final Map<String, Expr<?>> exprMap = new HashMap<>();
  private final Map<String, FuncDecl<?>> fieldFunctions = new HashMap<>();
  private final Log log = LogFactory.getLog("Z3Translator");

  public Z3Translator(final Context context) {
    this.context = context;
    this.sortInferrer = new Z3SortDetector(context);
  }

  public Map<String, Expr<?>> getDeclarations() {
    return Collections.unmodifiableMap(exprMap);
  }

  // entry point — translates a full constraint including truth value
  public BoolExpr translateConstraint(final SymbolicConstraint constraint) {
    try {
      Expr<?> expr = constraint.getSymExpr().accept(this);
      BoolExpr boolExpr = coerceToBool(expr);
      return constraint.getTruthValue() ? boolExpr : context.mkNot(boolExpr);
    } catch (Exception e) {
      log.error(
          "Failed on: "
              + constraint.getSymExpr().getClass().getSimpleName()
              + " = "
              + constraint.getSymExpr());
      throw e;
    }
  }

  private BoolExpr coerceToBool(final Expr<?> expr) {
    if (expr instanceof BoolExpr b) {
      return b;
    }
    return context.mkNot(context.mkEq(expr, context.mkInt(0)));
  }

  @Override
  public Expr<?> visitBinOp(final SymBinOp b) {
    Expr<?> left = b.getLhs().accept(this);
    Expr<?> right = b.getRhs().accept(this);

    if (b.getOp() == BinOp.EQ || b.getOp() == BinOp.NE) {
      return buildEqualityExpr(b.getOp(), left, right);
    }
    return buildArithExpr(b.getOp(), left, right);
  }

  private boolean isNullSentinel(final Expr<?> expr) {
    return expr.toString().equals(NULL_STR);
  }

  private BoolExpr buildEqualityExpr(final BinOp op, final Expr<?> left, final Expr<?> right) {
    if (isNullSentinel(right) || isNullSentinel(left)) {
      Expr<?> ref = isNullSentinel(right) ? left : right;
      // arr comes as |array_name:int[]|. we make it become array_name_is_null
      BoolExpr isNull =
          (BoolExpr) exprMap.computeIfAbsent(buildNullArrayName(ref), context::mkBoolConst);
      return op == BinOp.EQ ? isNull : context.mkNot(isNull);
    }

    var coerced = coerceForEquality(left, right);
    BoolExpr eq = context.mkEq(coerced.lhs(), coerced.rhs);
    return op == BinOp.EQ ? eq : context.mkNot(eq);
  }

  private String buildNullArrayName(final Expr<?> expr) {
    return expr.toString().replace("|", "").split(":")[0] + IS_NULL;
  }

  private record ExprPair(Expr<?> lhs, Expr<?> rhs) {}

  private ExprPair coerceForEquality(final Expr<?> lhs, final Expr<?> rhs) {
    Sort leftSort = lhs.getSort();
    Sort rightSort = rhs.getSort();
    if (leftSort.equals(rightSort)) {
      return new ExprPair(lhs, rhs);
    }

    UninterpretedSort objSort = context.mkUninterpretedSort(JAVA_LANG_OBJECT);
    Sort strSort = context.getStringSort();

    if (leftSort instanceof ArraySort<?, ?> ls
        && ls.getRange().equals(objSort)
        && rightSort.equals(strSort)) {
      return new ExprPair(context.mkSelect((ArrayExpr<IntSort, Sort>) lhs, context.mkInt(0)), rhs);
    }
    if (rightSort instanceof ArraySort<?, ?> rs
        && rs.getRange().equals(objSort)
        && leftSort.equals(strSort)) {
      return new ExprPair(lhs, context.mkSelect((ArrayExpr<IntSort, Sort>) rhs, context.mkInt(0)));
    }
    if (leftSort.equals(objSort) && rightSort.equals(strSort)) {
      return new ExprPair(coerceToString(lhs), rhs);
    }
    if (rightSort.equals(objSort) && leftSort.equals(strSort)) {
      return new ExprPair(lhs, coerceToString(rhs));
    }
    throw new IllegalStateException("Cannot compare sorts: " + leftSort + " vs " + rightSort);
  }

  private Expr<?> coerceToString(final Expr<?> expr) {
    return exprMap.computeIfAbsent(
        expr + AS_STR_SUFFIX, k -> context.mkConst(k, context.getStringSort()));
  }

  private Expr<?> buildArithExpr(final BinOp op, final Expr<?> lhs, final Expr<?> rhs) {
    return switch (op) {
      case LT -> context.mkLt((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case LE -> context.mkLe((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case GT -> context.mkGt((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case GE -> context.mkGe((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case ADD -> context.mkAdd((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case SUB -> context.mkSub((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case MUL -> context.mkMul((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case DIV -> context.mkDiv((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case MOD -> context.mkMod((IntExpr) lhs, (IntExpr) rhs);
      case CMP, CMPG, CMPL -> context.mkSub((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case SHIFT_LEFT -> context.mkBV2Int(context.mkBVSHL(bv(lhs), bv(rhs)), true);
      case SHIFT_RIGHT -> context.mkBV2Int(context.mkBVASHR(bv(lhs), bv(rhs)), true);
      case AND -> {
        if (rhs instanceof IntNum num) {
          int mask = num.getInt();
          if (mask > 0 && (mask & (mask + 1)) == 0) {
            // mask is 2^n - 1 — model as mod
            yield context.mkMod((IntExpr) lhs, context.mkInt(mask + 1));
          }
        }
        yield context.mkBV2Int(context.mkBVAND(bv(lhs), bv(rhs)), true);
      }
      case OR -> context.mkBV2Int(context.mkBVOR(bv(lhs), bv(rhs)), true);
      case UNSIGNED_SHIFT_RIGHT -> context.mkBV2Int(context.mkBVLSHR(bv(lhs), bv(rhs)), false);
      case XOR -> context.mkBV2Int(context.mkBVXOR(bv(lhs), bv(rhs)), true);
      default -> throw new IllegalStateException("Unhandled op in arith path: " + op);
    };
  }

  private BitVecExpr bv(final Expr<?> e) {
    return context.mkInt2BV(BITS_32, (IntExpr) e);
  }

  @Override
  public Expr<?> visitConst(final SymConst c) {
    if (c.getValue() == null) {
      // null as opaque integer 0 — for null comparisons. Revisit if necessary
      return context.mkInt(0);
    }
    return switch (c.getValue()) {
      case Integer i -> context.mkInt(i);
      case Long l -> context.mkInt(Long.toString(l));
      case Double d -> context.mkReal(d.toString());
      case Float f -> context.mkReal(f.toString());
      default -> context.mkInt(c.getValue().toString());
    };
  }

  @Override
  public Expr<?> visitIntConst(final SymIntConst i) {
    return context.mkInt(Integer.toString(i.getValue()));
  }

  @Override
  public Expr<?> visitDoubleConst(final SymDoubleConst d) {
    return context.mkReal(d.toString());
  }

  @Override
  public Expr<?> visitFloatConst(final SymFloatConst f) {
    return context.mkReal(f.toString());
  }

  @Override
  public Expr<?> visitLongConst(final SymLongConstant l) {
    return context.mkInt(Long.toString(l.getValue()));
  }

  @Override
  public Expr<?> visitFieldAccess(final SymFieldAccess f) {
    Expr<?> base = f.getBase().accept(this);
    String key = toFieldKEy(f, base);
    Expr<?> cached = exprMap.get(key);
    if (cached != null) {
      return cached;
    }

    Expr<?> result = translateFieldAccess(base, f.getFieldName());
    exprMap.put(key, result);
    return result;
  }

  @Override
  public Expr<?> visitStringConst(final SymStringConst c) {
    return context.mkString(c.getValue());
  }

  @Override
  public Expr<?> visitVar(final SymVar v) {
    return exprMap.computeIfAbsent(
        v.getName(),
        name -> {
          Sort sort = v.accept(sortInferrer);
          return context.mkConst(name, sort);
        });
  }

  private static String toFieldKEy(final SymFieldAccess f, final Expr<?> base) {
    return base.toString() + "." + f.getFieldName();
  }

  @Override
  public Expr<?> visitVirtualInvoke(final SymVirtualInvoke i) {
    return makeInvokeConst(i.toString(), i.getKind());
  }

  // Args are ignored for now since we're intraprocedural — the static invoke
  // is treated as an uninterpreted function returning a boolean or integer.
  @Override
  public Expr<?> visitStaticInvoke(final SymStaticInvoke i) {
    return makeInvokeConst(i.toString(), i.getKind());
  }

  @Override
  public Expr<?> visitInterfaceInvoke(final SymInterfaceInvoke i) {
    return makeInvokeConst(i.toString(), i.getKind());
  }

  @Override
  public Expr<?> visitDynamicInvoke(final SymDynamicInvoke d) {
    return exprMap.computeIfAbsent(
            "dynamicinvoke_" + d.getSignature()
                    .replaceAll("[^a-zA-Z0-9_]", "_"),
            context::mkIntConst);
  }

  @Override
  public Expr<?> visitSpecialInvoke(final SymSpecialInvoke i) {
    return makeInvokeConst(i.toString(), i.getKind());
  }

  private Expr<?> makeInvokeConst(final String key, final SymKind kind) {
    String typedKey = key + (kind == SymKind.BOOLEAN_METHOD ? "_bool" : "_int");
    Expr<?> expr = exprMap.computeIfAbsent(
            typedKey,
            k -> kind == SymKind.BOOLEAN_METHOD
                    ? context.mkBoolConst(k)
                    : context.mkIntConst(k));
    exprMap.put(key, expr);  // store under original key for model extraction
    return expr;
  }

  @Override
  public Expr<?> visitArray(final SymArray arr) {
    String cacheKey = toArrayKey(arr);
    Expr<?> cached = exprMap.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    ArraySort arraySort = (ArraySort) arr.accept(sortInferrer);
    Expr<?> result = context.mkConst(cacheKey, arraySort);
    exprMap.put(cacheKey, result);
    return result;
  }

  private static String toArrayKey(final SymArray arr) {
    return arr.getName() + ":" + arr.getObjectType();
  }

  @Override
  public Expr<?> visitArrayRef(final SymArrayRef ref) {
    String key = toArrayRefKey(ref);
    Expr<?> cached = exprMap.get(key);
    if (cached != null) {
      return cached;
    }

    Expr<?> arrayExpr = ref.getArray().accept(this);
    Expr<?> indexExpr = ref.getIndex().accept(this);
    Expr<?> result = context.mkSelect((ArrayExpr<IntSort, Sort>) arrayExpr, (IntExpr) indexExpr);
    exprMap.put(key, result);
    return result;
  }

  private static String toArrayRefKey(final SymArrayRef ref) {
    return ARRAY_SELECT + ref.toString();
  }

  @Override
  public Expr<?> visitLength(final SymLength l) {
    String key = l.toString();
    return exprMap.computeIfAbsent(key, context::mkIntConst);
  }

  @Override
  public Expr<?> visitCast(final SymCast c) {
    return exprMap.computeIfAbsent(c.toString(), context::mkIntConst);
  }

  @Override
  public Expr<?> visitInstanceOf(final SymInstanceOf i) {
    // for all we know so far, we only need a boolean in our tests
    String key = i.getOp().toString() + INSTANCEOF + i.getType().replace(".", "_");
    return exprMap.computeIfAbsent(key, context::mkBoolConst);
  }

  private Expr<?> translateFieldAccess(final Expr<?> base, final String fieldName) {

    FuncDecl<?> fieldDecl =
        fieldFunctions.computeIfAbsent(
            fieldName,
            f ->
                context.mkFuncDecl(
                    FIELD_DECL_PREFIX + f, new Sort[] {base.getSort()}, context.getIntSort()));

    return context.mkApp(fieldDecl, base);
  }

  @Override
  public Expr<?> visitParamRef(final SymParamRef r) {
    return exprMap.computeIfAbsent(
        r.toString(),
        name -> {
          Sort sort = r.accept(sortInferrer);
          return context.mkConst(name, sort);
        });
  }

  @Override
  public Expr<?> visitNull(final SymNull n) {
    return context.mkConst(NULL_STR, context.mkUninterpretedSort("Null"));
  }

  @Override
  public Expr<?> visitThisRef(final SymThisRef r) {
    return context.mkConst(THIS_STR, context.mkUninterpretedSort("This"));
  }

  @Override
  public Expr<?> visitCaughtException(final SymCaughtExceptionRef e) {
    String key = "caught_" + e.getCaughtType().replace(".", "_");
    return exprMap.computeIfAbsent(key, context::mkBoolConst);
  }

  @Override
  public Expr<?> visitNewRef(final SymNew n) {
    return exprMap.computeIfAbsent("new_" + n.toString().replace(".", "_"), context::mkIntConst);
  }

  @Override
  public Expr<?> visitStaticFieldRef(final SymStaticFieldRef r) {
    return exprMap.computeIfAbsent(
        r.getFieldSignature().replace(".", "_").replace(":", "_").replace(" ", "_"),
        k -> r.getKind() == SymKind.BOOLEAN ? context.mkBoolConst(k) : context.mkIntConst(k));
  }

  @Override
  public Expr<?> visitNeg(final SymNeg n) {
    return context.mkUnaryMinus((ArithExpr<IntSort>) n.getOperand().accept(this));
  }
}
