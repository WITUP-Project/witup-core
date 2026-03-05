package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.SeqExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;

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
    Map<String, String> model = status == Status.SATISFIABLE
            ? extractModel(solver.getModel())
            : Map.of();
    solver.pop();
    return new SolverResult(pathId, status, model);
  }

  private Map<String, String> extractModel(final Model model) {
    Map<String, String> result = new HashMap<>();
    for (FuncDecl<?> decl : model.getDecls()) {
      String name = decl.getName().toString();

      Expr<?> valueExpr = model.getConstInterp(decl);

      if (valueExpr == null) {
        continue;
      }

      String value;

      if (valueExpr instanceof SeqExpr<?> seq) {
        value = seq.getString();
      }
      else if (valueExpr.isIntNum()) {
        value = valueExpr.toString();
      }
      else if (valueExpr.isBool()) {
        value = valueExpr.toString();
      }
      else {
        // fallback
        value = valueExpr.toString();
      }
      result.put(name, value);
    }
    return result;
  }

  public void close() {
    ctx.close();
  }
}
