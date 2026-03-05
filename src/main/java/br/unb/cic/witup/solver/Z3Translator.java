package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.BinOp;
import br.unb.cic.witup.analysis.symbolic.SymArray;
import br.unb.cic.witup.analysis.symbolic.SymArrayRef;
import br.unb.cic.witup.analysis.symbolic.SymBinOp;
import br.unb.cic.witup.analysis.symbolic.SymCast;
import br.unb.cic.witup.analysis.symbolic.SymConst;
import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.SymField;
import br.unb.cic.witup.analysis.symbolic.SymInstanceOf;
import br.unb.cic.witup.analysis.symbolic.SymKind;
import br.unb.cic.witup.analysis.symbolic.SymLength;
import br.unb.cic.witup.analysis.symbolic.SymNewArray;
import br.unb.cic.witup.analysis.symbolic.SymStringConst;
import br.unb.cic.witup.analysis.symbolic.SymVar;
import br.unb.cic.witup.analysis.symbolic.SymVirtualInvoke;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Sort;
import com.microsoft.z3.UninterpretedSort;
import com.microsoft.z3.enumerations.Z3_sort_kind;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_SEQ_SORT;
import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_UNINTERPRETED_SORT;

public final class Z3Translator implements SymExprVisitor<Expr<?>> {
  private final Context context;
  private final Z3SortDetector sortInferrer;
  private final Map<String, Expr<?>> cache = new HashMap<>();
  private final UninterpretedSort objectSort;

  public Z3Translator(final Context context) {
    this.context = context;
    this.sortInferrer = new Z3SortDetector(context);
    this.objectSort = context.mkUninterpretedSort("java.lang.Object");
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

      Z3_sort_kind leftKind = left.getSort().getSortKind();
      Z3_sort_kind rightKind = right.getSort().getSortKind();

      if (!left.getSort().equals(right.getSort())) {
        // Automatic coercion if comparing OBJECT array element to STRING
        if (leftSort instanceof ArraySort leftArrSort &&
                leftArrSort.getRange().equals(objectSort) &&
                rightSort.equals(context.getStringSort())) {

          left = context.mkSelect((ArrayExpr<IntSort, Sort>) left, context.mkInt(0));

        } else if (rightSort instanceof ArraySort rightArrSort &&
                rightArrSort.getRange().equals(objectSort) &&
                leftSort.equals(context.getStringSort())) {

          right = context.mkSelect((ArrayExpr<IntSort, Sort>) right, context.mkInt(0));

        } else {
          throw new IllegalStateException(
                  "Cannot compare sorts: " + leftSort + " vs " + rightSort);
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
  public Expr<?> visitField(final SymField f) {
    // treat as opaque integer constant named "base_field" for now
    String key = f.toString();
    return cache.computeIfAbsent(key, context::mkIntConst);
  }

  @Override
  public Expr<?> visitVirtualInvoke(final SymVirtualInvoke inv) {
    return cache.computeIfAbsent(
        inv.toString(),
        k -> inv.kind() == SymKind.BOOLEAN_METHOD ? context.mkBoolConst(k) : context.mkIntConst(k));
  }

  @Override
  public Expr<?> visitArray(SymArray arr) {
    ArraySort arraySort = (ArraySort) arr.accept(sortInferrer);
    return context.mkConst(arr.getName(), arraySort);
  }

  @Override
  public Expr<?> visitArrayRef(SymArrayRef ref) {
    Expr<?> arrayExprRaw = ref.getArray().accept(this);
    Expr<?> indexExprRaw = ref.getIndex().accept(this);

    if (!(arrayExprRaw instanceof ArrayExpr<?, ?> arrayExpr)) {
      throw new IllegalStateException(
              "Expected ArrayExpr for array base, got " + arrayExprRaw.getClass()
      );
    }
    if (!(indexExprRaw instanceof IntExpr indexExpr)) {
      throw new IllegalStateException(
              "Expected IntExpr for array index, got " + indexExprRaw.getClass()
      );
    }
    // array index is always sort
    ArrayExpr<IntSort, Sort> safeArray = (ArrayExpr<IntSort, Sort>) arrayExpr;

    return context.mkSelect(safeArray, indexExpr);
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
    return context.mkBoolConst(
        i.getOp().toString() + "_instanceof_" + i.getType().replace(".", "_"));
  }
}
