package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymArray;
import br.unb.cic.witup.analysis.symbolic.expr.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymCast;
import br.unb.cic.witup.analysis.symbolic.expr.SymCaughtExceptionRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymClassConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymDoubleConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymDynamicInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymFieldAccess;
import br.unb.cic.witup.analysis.symbolic.expr.SymFloatConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymITE;
import br.unb.cic.witup.analysis.symbolic.expr.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.expr.SymIntConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymInterfaceInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymLength;
import br.unb.cic.witup.analysis.symbolic.expr.SymLongConstant;
import br.unb.cic.witup.analysis.symbolic.expr.SymNeg;
import br.unb.cic.witup.analysis.symbolic.expr.SymNew;
import br.unb.cic.witup.analysis.symbolic.expr.SymNewMultiArray;
import br.unb.cic.witup.analysis.symbolic.expr.SymNull;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymSpecialInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticFieldRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymStaticInvoke;
import br.unb.cic.witup.analysis.symbolic.expr.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.expr.SymThisRef;
import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.expr.SymVirtualInvoke;
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
import java.util.IdentityHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class Z3Translator implements SymExprVisitor<Expr<?>> {
  private final Log log = LogFactory.getLog("Z3Translator");
  public static final String JAVA_LANG_OBJECT = "java.lang.Object";
  public static final String AS_STR_SUFFIX = "_as_str";
  public static final String INSTANCEOF = "_instanceof_";
  public static final String FIELD_DECL_PREFIX = "field_";
  public static final String IS_NULL = "_is_null";
  public static final String NULL_STR = "__null__";
  public static final String THIS_STR = "__this__";
  public static final int BITS_32 = 32;
  public static final String BOOL_SUFFIX = "_bool";
  public static final String INT_SUFFIX = "_int";
  public static final String CLASS_PREFIX = "class_";
  private final Context context;
  private final Z3SortDetector sortInferrer;
  private final Map<String, Expr<?>> exprMap = new HashMap<>();
  private final Map<String, FuncDecl<?>> fieldFunctions = new HashMap<>();
  public static final int MAX_DESCRIPTION_CHARS = 256;
  private final Map<SymExpr, Expr<?>> exprCache = new IdentityHashMap<>(2048);
  private final Map<Expr<?>, Sort> sortCache = new IdentityHashMap<>(2048);
  private final Map<SymExpr, String> exprIds = new IdentityHashMap<>(2048);
  private final Map<String, String> idToTruncatedDescription = new HashMap<>(2048);
  private final Map<Integer, IntNum> intConstCache = new HashMap<>(2048);
  private int exprCounter = 0;
  private final IntNum zero;
  private final IntNum one;

  public Z3Translator(final Context context) {
    this.context = context;
    this.sortInferrer = new Z3SortDetector(context);
    this.zero = context.mkInt(0);
    this.one = context.mkInt(1);
    intConstCache.put(0, zero);
    intConstCache.put(1, one);
  }

  /**
   * Clears per-path state (declarations, IDs, descriptions) while preserving
   * cross-path caches (exprCache, sortCache, intConstCache).
   */
  public void resetForNewPath() {
    exprMap.clear();
    fieldFunctions.clear();
    exprIds.clear();
    idToTruncatedDescription.clear();
    exprCounter = 0;
  }

  // entry point — translates a full constraint including truth value
  public BoolExpr translateConstraint(final SymbolicConstraint constraint) {
    try {
      Expr<?> expr = translate(constraint.symExpr());
      BoolExpr boolExpr = coerceToBool(expr);
      return constraint.truthValue() ? boolExpr : context.mkNot(boolExpr);
    } catch (Exception e) {
      log.error(
          "Failed on: "
              + constraint.symExpr().getClass().getSimpleName()
              + " = "
              + idFor(constraint.symExpr()));
      throw e;
    }
  }

  public Expr<?> translate(final SymExpr expr) {
    return exprCache.computeIfAbsent(expr, e -> e.accept(this));
  }

  public Map<String, Expr<?>> getDeclarations() {
    return Collections.unmodifiableMap(exprMap);
  }

  private BoolExpr coerceToBool(final Expr<?> expr) {
    if (expr instanceof BoolExpr b) {
      return b;
    }
    return context.mkNot(context.mkEq(expr, zero));
  }

  private Sort sortOf(final Expr<?> e) {
    return sortCache.computeIfAbsent(e, Expr::getSort);
  }

  @Override
  public Expr<?> visitBinOp(final SymBinOp b) {
    Expr<?> left = translate(b.getLhs());
    Expr<?> right = translate(b.getRhs());

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
    Sort leftSort = sortOf(lhs);
    Sort rightSort = sortOf(rhs);
    if (leftSort.equals(rightSort)) {
      return new ExprPair(lhs, rhs);
    }

    Sort nullSort = context.mkUninterpretedSort("Null");
    Sort intSort = context.getIntSort();
    Sort strSort = context.getStringSort();
    Sort realSort = context.getRealSort();

    if (lhs instanceof BoolExpr boolLhs && rightSort.equals(context.getIntSort())) {
      return new ExprPair(toArith(boolLhs), rhs);
    }
    if (rhs instanceof BoolExpr boolRhs && leftSort.equals(context.getIntSort())) {
      return new ExprPair(lhs, toArith(boolRhs));
    }
    // Null vs Int — encode as null check bool const, coerce to int
    if (leftSort.equals(nullSort) && rightSort.equals(intSort)) {
      String key = lhs.toString().replace("|", "") + IS_NULL;
      BoolExpr isNull = (BoolExpr) exprMap.computeIfAbsent(key, context::mkBoolConst);
      return new ExprPair(toArith(isNull), rhs);
    }
    if (rightSort.equals(nullSort) && leftSort.equals(intSort)) {
      String key = rhs.toString().replace("|", "") + IS_NULL;
      BoolExpr isNull = (BoolExpr) exprMap.computeIfAbsent(key, context::mkBoolConst);
      return new ExprPair(lhs, toArith(isNull));
    }

    // String vs Int — coerce string to opaque int
    if (leftSort.equals(strSort) && rightSort.equals(intSort)) {
      Expr<?> coerced = exprMap.computeIfAbsent(lhs + "_as_int", context::mkIntConst);
      return new ExprPair(coerced, rhs);
    }
    if (rightSort.equals(strSort) && leftSort.equals(intSort)) {
      Expr<?> coerced = exprMap.computeIfAbsent(rhs + "_as_int", context::mkIntConst);
      return new ExprPair(lhs, coerced);
    }

    UninterpretedSort objSort = context.mkUninterpretedSort(JAVA_LANG_OBJECT);

    if (leftSort instanceof ArraySort<?, ?> ls
        && ls.getRange().equals(objSort)
        && rightSort.equals(strSort)) {
      return new ExprPair(context.mkSelect((ArrayExpr<IntSort, Sort>) lhs, zero), rhs);
    }
    if (rightSort instanceof ArraySort<?, ?> rs
        && rs.getRange().equals(objSort)
        && leftSort.equals(strSort)) {
      return new ExprPair(lhs, context.mkSelect((ArrayExpr<IntSort, Sort>) rhs, zero));
    }
    if (leftSort.equals(objSort) && rightSort.equals(strSort)) {
      return new ExprPair(coerceToString(lhs), rhs);
    }
    if (rightSort.equals(objSort) && leftSort.equals(strSort)) {
      return new ExprPair(lhs, coerceToString(rhs));
    }
    // lhs Int, rhs Real
    if (leftSort.equals(intSort) && rightSort.equals(realSort)) {
      Expr<?> lhsAsReal = context.mkInt2Real((IntExpr) lhs);
      return new ExprPair(lhsAsReal, rhs);
    }

    // lhs Real, rhs Int
    if (leftSort.equals(realSort) && rightSort.equals(intSort)) {
      Expr<?> rhsAsReal = context.mkInt2Real((IntExpr) rhs);
      return new ExprPair(lhs, rhsAsReal);
    }
    throw new IllegalStateException("Cannot compare sorts: " + leftSort + " vs " + rightSort);
  }

  private Expr<?> coerceToString(final Expr<?> expr) {
    return exprMap.computeIfAbsent(
        expr + AS_STR_SUFFIX, k -> context.mkConst(k, context.getStringSort()));
  }

  private Expr<?> buildArithExpr(final BinOp op, final Expr<?> lhs, final Expr<?> rhs) {
    return switch (op) {
      case LT -> context.mkLt(toArith(lhs), toArith(rhs));
      case LE -> context.mkLe(toArith(lhs), toArith(rhs));
      case GT -> context.mkGt(toArith(lhs), toArith(rhs));
      case GE -> context.mkGe(toArith(lhs), toArith(rhs));
      case ADD -> context.mkAdd(toArith(lhs), toArith(rhs));
      case SUB -> context.mkSub(toArith(lhs), toArith(rhs));
      case MUL -> context.mkMul(toArith(lhs), toArith(rhs));
      case DIV -> context.mkDiv(toArith(lhs), toArith(rhs));
      case MOD -> context.mkMod((IntExpr) lhs, (IntExpr) rhs);
      case CMP, CMPG, CMPL -> context.mkSub((ArithExpr<IntSort>) lhs, (ArithExpr<IntSort>) rhs);
      case SHIFT_LEFT -> context.mkBV2Int(context.mkBVSHL(bv(lhs), bv(rhs)), true);
      case SHIFT_RIGHT -> context.mkBV2Int(context.mkBVASHR(bv(lhs), bv(rhs)), true);
      case AND -> {
        // hacky but speeds up some Z3 paths massively. Need to come back here
        // and reconsider when we have better understood the best ways to
        // reprent each kind of symbolic constraint in Z3. Right now this
        // entire layer might be sub-optimal
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

  @SuppressWarnings("unchecked")
  private ArithExpr<IntSort> toArith(final Expr<?> expr) {
    if (expr instanceof BoolExpr b) {
      return (ArithExpr<IntSort>) context.mkITE(b, one, zero);
    }
    Sort sort = sortOf(expr);
    if (sort.equals(context.getIntSort()) || sort.equals(context.getRealSort())) {
      return (ArithExpr<IntSort>) expr;
    }
    // Null, String, UninterpretedSort — encode as opaque int constant
    String key = expr.toString().replace("|", "") + "_as_int";
    return (ArithExpr<IntSort>) exprMap.computeIfAbsent(key, context::mkIntConst);
  }

  private BitVecExpr bv(final Expr<?> e) {
    return context.mkInt2BV(BITS_32, (IntExpr) e);
  }

  @Override
  public Expr<?> visitConst(final SymConst c) {
    if (c.getValue() == null) {
      // null as opaque integer 0 — for null comparisons. Revisit if necessary
      return zero;
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
    return intConstCache.computeIfAbsent(i.getValue(), v -> context.mkInt(Integer.toString(v)));
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
    Expr<?> base = translate(f.getBase());
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
    return makeInvokeConst(i, i.getKind());
  }

  // const is enough for our purposes. the symbolic constraint generator
  // resolves them recursively before we get here
  @Override
  public Expr<?> visitStaticInvoke(final SymStaticInvoke i) {
    return makeInvokeConst(i, i.getKind());
  }

  @Override
  public Expr<?> visitInterfaceInvoke(final SymInterfaceInvoke i) {
    return makeInvokeConst(i, i.getKind());
  }

  @Override
  public Expr<?> visitDynamicInvoke(final SymDynamicInvoke d) {
    return exprMap.computeIfAbsent(
        "dynamicinvoke_" + d.getSignature().replaceAll("[^a-zA-Z0-9_]", "_"), context::mkIntConst);
  }

  @Override
  public Expr<?> visitSpecialInvoke(final SymSpecialInvoke i) {
    return makeInvokeConst(i, i.getKind());
  }

  private Expr<?> makeInvokeConst(final SymExpr invokeExpr, final SymKind kind) {
    String id = idFor(invokeExpr);
    String typedKey = id + (kind == SymKind.BOOLEAN_METHOD ? BOOL_SUFFIX : INT_SUFFIX);

    Expr<?> expr =
        exprMap.computeIfAbsent(
            typedKey,
            k -> kind == SymKind.BOOLEAN_METHOD ? context.mkBoolConst(k) : context.mkIntConst(k));
    exprMap.put(id, expr); // store under original key for model extraction
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
    String key = idFor(ref);
    Expr<?> cached = exprMap.get(key);
    if (cached != null) {
      return cached;
    }

    Expr<?> arrayExpr = translate(ref.getArray());
    Expr<?> indexExpr = translate(ref.getIndex());
    Expr<?> result = context.mkSelect((ArrayExpr<IntSort, Sort>) arrayExpr, (IntExpr) indexExpr);
    exprMap.put(key, result);
    return result;
  }

  @Override
  public Expr<?> visitNewMultiArray(final SymNewMultiArray e) {
    return exprMap.computeIfAbsent(idFor(e), context::mkIntConst);
  }

  @Override
  public Expr<?> visitLength(final SymLength l) {
    // String is safe; it will not blow up recursively
    return exprMap.computeIfAbsent(l.toString(), context::mkIntConst);
  }

  @Override
  public Expr<?> visitCast(final SymCast c) {
    // String is safe; it will not blow up recursively
    return exprMap.computeIfAbsent(idFor(c), context::mkIntConst);
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
                    FIELD_DECL_PREFIX + f, new Sort[] {sortOf(base)}, context.getIntSort()));

    return context.mkApp(fieldDecl, base);
  }

  @Override
  public Expr<?> visitParamRef(final SymParamRef r) {
    return exprMap.computeIfAbsent(
        idFor(r),
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
    return exprMap.computeIfAbsent(idFor(n), context::mkIntConst);
  }

  @Override
  public Expr<?> visitStaticFieldRef(final SymStaticFieldRef r) {
    return exprMap.computeIfAbsent(
        r.getFieldSignature().replace(".", "_").replace(":", "_").replace(" ", "_"),
        k -> r.getKind() == SymKind.BOOLEAN ? context.mkBoolConst(k) : context.mkIntConst(k));
  }

  @Override
  public Expr<?> visitNeg(final SymNeg n) {
    return context.mkUnaryMinus((ArithExpr<IntSort>) translate(n.getOperand()));
  }

  @Override
  public Expr<?> visitClassConst(final SymClassConst c) {
    return exprMap.computeIfAbsent(
        CLASS_PREFIX + c.getValue().replace("/", "_").replace(";", "").replace("[", ""),
        context::mkIntConst);
  }

  @Override
  public Expr<?> visitITE(final SymITE ite) {
    Expr<?> condExprRaw = translate(ite.getCondition());
    Expr<?> thenExpr = translate(ite.getThenExpr());
    Expr<?> elseExpr = translate(ite.getElseExpr());

    // coerce condition to BoolExpr if it is an IntExpr
    BoolExpr condExpr;
    if (condExprRaw instanceof BoolExpr b) {
      condExpr = b;
    } else if (condExprRaw instanceof ArithExpr<?> a) {
      // condition is integer: nonzero -> true
      condExpr = context.mkNot(context.mkEq(a, zero));
    } else {
      throw new IllegalStateException("Unexpected ITE condition type: " + condExprRaw.getClass());
    }

    Sort thenSort = sortOf(thenExpr);
    Sort elseSort = sortOf(elseExpr);
    if (!thenSort.equals(elseSort)) {
      thenExpr = toArith(thenExpr);
      elseExpr = toArith(elseExpr);
    }

    return context.mkITE(condExpr, thenExpr, elseExpr);
  }

  private String idFor(final SymExpr expr) {
    return exprIds.computeIfAbsent(
        expr,
        e -> {
          String id = "expr_" + exprCounter++;
          String raw = describeExpr(e);
          idToTruncatedDescription.put(
              id,
              raw.length() > MAX_DESCRIPTION_CHARS
                  ? raw.substring(0, MAX_DESCRIPTION_CHARS) + "..."
                  : raw);
          return id;
        });
  }

  private static String describeExpr(final SymExpr expr) {
    return switch (expr) {
      case SymVar v -> v.getName();
      case SymParamRef p -> p.toString();
      case SymLength l -> safeDescribe(l.getOp()) + ".length()";
      case SymCast c -> "(" + c.getType() + ")" + safeDescribe(c.getOp());
      case SymInstanceOf i ->
          safeDescribe(i.getOp()) + "_instanceof_" + i.getType().replace(".", "_");
      case SymVirtualInvoke i -> {
        StringBuilder sb = new StringBuilder(safeDescribe(i.getBase()));
        sb.append(".").append(i.getSignature()).append("(");
        SymExpr[] args = i.getArgs();
        if (args.length > 0) {
          sb.append(safeDescribe(args[0]));
          for (int k = 1; k < args.length; k++) {
            sb.append(",").append(safeDescribe(args[k]));
          }
        }
        sb.append(")");
        yield sb.toString();
      }
      default -> expr.getClass().getSimpleName() + "@" + System.identityHashCode(expr);
    };
  }

  private static String safeDescribe(final SymExpr expr) {
    return switch (expr) {
      case SymVar v -> v.getName();
      case SymParamRef p -> p.toString();
      default -> expr.getClass().getSimpleName() + "@" + System.identityHashCode(expr);
    };
  }

  public Map<String, String> getIdDescriptions() {
    return Collections.unmodifiableMap(idToTruncatedDescription);
  }
}
