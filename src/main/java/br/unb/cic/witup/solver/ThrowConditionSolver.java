package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.enumerations.Z3_sort_kind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_BOOL_SORT;
import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_INT_SORT;
import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_SEQ_SORT;
import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_ARRAY_SORT;


public final class ThrowConditionSolver {
  private final Context ctx;
  private final Solver solver;

  public ThrowConditionSolver() {
    this.ctx = new Context();
    this.solver = ctx.mkSolver();
  }

  public Context getContext() {
    return ctx;
  }

  public SolverResult check(final String pathId, final List<SymbolicConstraint> constraints) {
    Z3Translator translator = new Z3Translator(ctx);

    solver.push();

    for (SymbolicConstraint c : constraints) {
      solver.add(translator.translateConstraint(c));
    }
    Status status = solver.check();

    Model z3Model = null;
    Map<String, ModelValue> modelValues = Map.of();

    if (status == Status.SATISFIABLE) {
      z3Model = solver.getModel();
      modelValues = extractModel(z3Model);
    }

    solver.pop();
    return new SolverResult(pathId, status, modelValues, ctx, z3Model);
  }

  private Map<String, ModelValue> extractModel(final Model model) {
    Map<String, ModelValue> result = new HashMap<>();

    for (FuncDecl<?> decl : model.getDecls()) {
      String name = decl.getName().toString();
      Expr<?> expr = model.getConstInterp(decl);

      ModelValue value = null;
      Z3_sort_kind sortKind = expr.getSort().getSortKind();

      if (sortKind == Z3_BOOL_SORT) {
        value = new ModelValue.BoolValue(expr.isTrue());
      } else if (sortKind == Z3_INT_SORT) {
        value = new ModelValue.IntValue(Integer.parseInt(expr.toString()));
      } else if (sortKind == Z3_SEQ_SORT) {
        value = new ModelValue.StringValue(expr.getString());
      } else if (sortKind == Z3_ARRAY_SORT) {
        value = new ModelValue.ArrayValue((ArrayExpr<IntSort, ?>) expr, model, ctx);
      } else {
        throw new IllegalStateException("Unsupported sort: " + expr.getSort());
      }
      result.put(name, value);
    }
    return result;
  }

  public void close() {
    ctx.close();
  }
}
