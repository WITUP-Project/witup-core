package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.BinOp;
import br.unb.cic.witup.analysis.symbolic.SymArray;
import br.unb.cic.witup.analysis.symbolic.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymCast;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymFieldAccess;
import br.unb.cic.witup.analysis.symbolic.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.SymLength;
import br.unb.cic.witup.analysis.symbolic.SymParam;
import br.unb.cic.witup.analysis.symbolic.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.SymVar;
import br.unb.cic.witup.analysis.symbolic.SymVirtualInvoke;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Sort;
import com.microsoft.z3.UninterpretedSort;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Z3Translator implements SymExprVisitor<Expr<?>> {
  public static final String JAVA_LANG_OBJECT = "java.lang.Object";
  public static final String AS_STR_SUFFIX = "_as_str";
  public static final String INSTANCEOF = "_instanceof_";
  public static final String FIELD_DECL_PREFIX = "field_";
  public static final String ARRAY_SELECT = "select:";
  private final Context context;
  private final Z3SortDetector sortInferrer;
  private final Map<String, Expr<?>> exprMap = new HashMap<>();
  private final Map<String, FuncDecl<?>> fieldFunctions = new HashMap<>();

  public Z3Translator(final Context context) {
    this.context = context;
    this.sortInferrer = new Z3SortDetector(context);
  }

  public Map<String, Expr<?>> getDeclarations() {
    return Collections.unmodifiableMap(exprMap);
  }

  // entry point — translates a full constraint including truth value
  public BoolExpr translateConstraint(final SymbolicConstraint constraint) {
    Expr<?> expr = constraint.getSymExpr().accept(this);
    BoolExpr boolExpr = coerceToBool(expr);
    return constraint.getTruthValue() ? boolExpr : context.mkNot(boolExpr);
  }

  private BoolExpr coerceToBool(final Expr<?> expr) {
    if (expr instanceof BoolExpr b) {
      return b;
    }
    return context.mkNot(context.mkEq(expr, context.mkInt(0)));
  }

  @Override
  public Expr<?> visitBinOp(final SymBinOp b) {
    Expr<?> left = b.getLeft().accept(this);
    Expr<?> right = b.getRight().accept(this);

    if (b.getOp() == BinOp.EQ || b.getOp() == BinOp.NE) {
      return buildEqualityExpr(b.getOp(), left, right);
    }
    return buildArithExpr(b.getOp(), left, right);
  }

  private BoolExpr buildEqualityExpr(final BinOp op, final Expr<?> left, final Expr<?> right) {
    var coerced = coerceForEquality(left, right);
    BoolExpr eq = context.mkEq(coerced.lhs(), coerced.rhs);
    return op == BinOp.EQ ? eq : context.mkNot(eq);
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

  private Expr<?> buildArithExpr(final BinOp op, final Expr<?> left, final Expr<?> right) {
    return switch (op) {
      case LT -> context.mkLt((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case LE -> context.mkLe((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case GT -> context.mkGt((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case GE -> context.mkGe((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case ADD -> context.mkAdd((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case SUB -> context.mkSub((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case MUL -> context.mkMul((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case DIV -> context.mkDiv((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case MOD -> context.mkMod((IntExpr) left, (IntExpr) right);
      case CMP, CMPG, CMPL -> context.mkSub((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      default -> throw new IllegalStateException("Unhandled op in arith path: " + op);
    };
  }

  @Override
  public Expr<?> visitConst(final SymConst c) {
    if (c.getValue() == null) {
      // null as opaque integer 0 — for null comparisons. Revisit if necessary
      return context.mkInt(0);
    }
    return switch (c.getValue()) {
      case Integer i -> context.mkInt(i);
      case Long l -> context.mkInt(l);
      case Double d -> context.mkReal(d.toString());
      case Float f -> context.mkReal(f.toString());
      default -> context.mkInt(c.getValue().toString());
    };
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
  public Expr<?> visitVirtualInvoke(final SymVirtualInvoke inv) {
    return exprMap.computeIfAbsent(
        inv.toString(),
        k -> inv.kind() == SymKind.BOOLEAN_METHOD ? context.mkBoolConst(k) : context.mkIntConst(k));
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
  public Expr<?> visitParamRef(final SymParam r) {
    return exprMap.computeIfAbsent(
        r.toString(),
        name -> {
          Sort sort = r.accept(sortInferrer);
          return context.mkConst(name, sort);
        });
  }
}
