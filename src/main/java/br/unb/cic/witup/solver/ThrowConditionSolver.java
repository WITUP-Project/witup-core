package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.CharSort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.enumerations.Z3_sort_kind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ThrowConditionSolver {
  private final Context ctx;
  private final Solver solver;

  public ThrowConditionSolver() {
    this.ctx = new Context();
    this.solver = ctx.mkSolver();
  }

  public SolverResult check(
          final String pathId,
          final List<SymbolicConstraint> constraints) {
    Z3Translator translator = new Z3Translator(ctx);
    solver.push();
    for (SymbolicConstraint c : constraints) {
      solver.add(translator.translateConstraint(c));
    }
    Status status = solver.check();
    Map<String, ModelValue> model = status == Status.SATISFIABLE
            ? extractModel(solver.getModel())
            : Map.of();
    solver.pop();
    return new SolverResult(pathId, status, model);
  }

  private Map<String, ModelValue> extractModel(final Model model) {
    Map<String, ModelValue> result = new HashMap<>();

    for (FuncDecl<?> decl : model.getDecls()) {
      String name = decl.getName().toString();
      Expr<?> expr = model.getConstInterp(decl);

      ModelValue value;
      switch (expr.getSort().getSortKind()) {
        case Z3_BOOL_SORT -> value = new ModelValue.BoolValue(expr.isTrue());
        case Z3_INT_SORT  -> value = new ModelValue.IntValue(Integer.parseInt(expr.toString()));
        case Z3_SEQ_SORT -> value = new ModelValue.StringValue(expr.getString());
        default -> throw new IllegalStateException("Unsupported sort: " + expr.getSort());
      }
      result.put(name, value);
    }
    return result;
  }

  public void close() {
    ctx.close();
  }
}
