package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.BinOp;
import br.unb.cic.witup.analysis.symbolic.SymArray;
import br.unb.cic.witup.analysis.symbolic.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymCast;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymFieldAccess;
import br.unb.cic.witup.analysis.symbolic.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.SymLength;
import br.unb.cic.witup.analysis.symbolic.SymNewArray;
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
  private final Context context;
  private final Z3SortDetector sortInferrer;
  private final Map<String, Expr<?>> cache = new HashMap<>();
  private final Map<String, FuncDecl<?>> fieldFunctions = new HashMap<>();

  public Z3Translator(final Context context) {
    this.context = context;
    this.sortInferrer = new Z3SortDetector(context);
  }

  public Map<String, Expr<?>> getDeclarations() {
    return Collections.unmodifiableMap(cache);
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
  @SuppressWarnings("unchecked")
  public Expr<?> visitBinOp(final SymBinOp b) {
    Expr<?> left = b.getLeft().accept(this);
    Expr<?> right = b.getRight().accept(this);

    if (b.getOp() == BinOp.EQ || b.getOp() == BinOp.NE) {
      Sort leftSort = left.getSort();
      Sort rightSort = right.getSort();

      if (!leftSort.equals(rightSort)) {
        UninterpretedSort objSort = context.mkUninterpretedSort("java.lang.Object");

        if (leftSort instanceof ArraySort ls
            && ls.getRange().equals(objSort)
            && rightSort.equals(context.getStringSort())) {
          left = context.mkSelect((ArrayExpr<IntSort, Sort>) left, context.mkInt(0));

        } else if (rightSort instanceof ArraySort rs
            && rs.getRange().equals(objSort)
            && leftSort.equals(context.getStringSort())) {
          right = context.mkSelect((ArrayExpr<IntSort, Sort>) right, context.mkInt(0));

        } else if (leftSort.equals(objSort) && rightSort.equals(context.getStringSort())) {
          System.out.println("coercion key: " + left.toString() + "_as_str");
          left =
              cache.computeIfAbsent(
                  left.toString() + "_as_str", k -> context.mkConst(k, context.getStringSort()));

        } else if (rightSort.equals(objSort) && leftSort.equals(context.getStringSort())) {
          right =
              cache.computeIfAbsent(
                  right.toString() + "_as_str", k -> context.mkConst(k, context.getStringSort()));

        } else {
          throw new IllegalStateException("Cannot compare sorts: " + leftSort + " vs " + rightSort);
        }
      }
      BoolExpr eq = context.mkEq(left, right);
      return b.getOp() == BinOp.EQ ? eq : context.mkNot(eq);
    }

    //    Sort sort = b.getLeft().accept(sortInferrer);

    //    if (sort.equals(context.getRealSort())) {
    //      ArithExpr<RealSort> lReal = (ArithExpr<RealSort>) left;
    //      ArithExpr<RealSort> rReal = (ArithExpr<RealSort>) right;
    //      return switch (b.getOp()) {
    //        case LT  -> context.mkLt(lReal, rReal);
    //        case ADD -> context.mkAdd(lReal, rReal);
    //        // ...
    //        default  -> context.mkEq(left, right);
    //      };
    //    }

    // default: integer path
    //    ArithExpr<IntSort> lInt = (ArithExpr<IntSort>) left;
    //    ArithExpr<IntSort> rInt = (ArithExpr<IntSort>) right;

    return switch (b.getOp()) {
        // comparison — produce BoolExpr
      case EQ -> context.mkEq(left, right);
      case NE -> context.mkNot(context.mkEq(left, right));
      case LT -> context.mkLt((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case LE -> context.mkLe((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case GT -> context.mkGt((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case GE -> context.mkGe((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
        // arithmetic — produce ArithExpr
      case ADD -> context.mkAdd((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case SUB -> context.mkSub((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case MUL -> context.mkMul((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case DIV -> context.mkDiv((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
      case MOD -> context.mkMod((IntExpr) left, (IntExpr) right);
        // cmp patterns already simplified by simplifyCmpPatterns
        // these should not reach here in normal operation
      case CMP, CMPG, CMPL -> context.mkSub((ArithExpr<IntSort>) left, (ArithExpr<IntSort>) right);
    };
  }

  @Override
  public Expr<?> visitVar(final SymVar v) {
    return cache.computeIfAbsent(
        v.getName(),
        name -> {
          Sort sort = v.accept(sortInferrer);
          return context.mkConst(name, sort);
        });
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
  public Expr<?> visitStringConst(final SymStringConst c) {
    return context.mkString(c.getValue());
  }

  @Override
  public Expr<?> visitField(final SymFieldAccess f) {
    Expr<?> base = f.getBase().accept(this);
    String key = base.toString() + "." + f.getFieldName();
    Expr<?> cached = cache.get(key);
    if (cached != null) {
      return cached;
    }

    Expr<?> result = translateFieldAccess(base, f.getFieldName());
    cache.put(key, result);
    return result;
  }

  @Override
  public Expr<?> visitVirtualInvoke(final SymVirtualInvoke inv) {
    return cache.computeIfAbsent(
        inv.toString(),
        k -> inv.kind() == SymKind.BOOLEAN_METHOD ? context.mkBoolConst(k) : context.mkIntConst(k));
  }

  @Override
  public Expr<?> visitArray(final SymArray arr) {
    String cacheKey = arr.getName() + ":" + arr.getObjectType();
    Expr<?> cached = cache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    ArraySort arraySort = (ArraySort) arr.accept(sortInferrer);
    Expr<?> result = context.mkConst(cacheKey, arraySort);
    cache.put(cacheKey, result);
    return result;
  }

  @Override
  public Expr<?> visitArrayRef(final SymArrayRef ref) {
    String key = "select:" + ref.toString();
    Expr<?> cached = cache.get(key);
    if (cached != null) {
      return cached;
    }

    Expr<?> arrayExpr = ref.getArray().accept(this);
    Expr<?> indexExpr = ref.getIndex().accept(this);
    Expr<?> result = context.mkSelect((ArrayExpr<IntSort, Sort>) arrayExpr, (IntExpr) indexExpr);
    cache.put(key, result);
    return result;
  }

  @Override
  public Expr<?> visitLength(final SymLength l) {
    String key = l.toString();
    return cache.computeIfAbsent(key, context::mkIntConst);
  }

  @Override
  public Expr<?> visitNewArray(final SymNewArray r) {
    String key = r.toString();
    return cache.computeIfAbsent(key, context::mkIntConst);
  }

  @Override
  public Expr<?> visitCast(final SymCast c) {
    return cache.computeIfAbsent(c.toString(), context::mkIntConst);
  }

  @Override
  public Expr<?> visitInstanceOf(final SymInstanceOf i) {
    // for all we know so far, we only need a boolean in our tests
    String key = i.getOp().toString() + "_instanceof_" + i.getType().replace(".", "_");
    return cache.computeIfAbsent(key, context::mkBoolConst);
  }

  private Expr<?> translateFieldAccess(final Expr<?> base, final String fieldName) {

    FuncDecl<?> fieldDecl =
        fieldFunctions.computeIfAbsent(
            fieldName,
            f ->
                context.mkFuncDecl(
                    "field_" + f, new Sort[] {base.getSort()}, context.getIntSort()));

    return context.mkApp(fieldDecl, base);
  }
}
