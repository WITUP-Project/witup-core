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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_ARRAY_SORT;


public final class ThrowConditionSolver {
    public SolverResult check(final String pathId, final List<SymbolicConstraint> constraints) {
    Context ctx = new Context();
    Solver solver = ctx.mkSolver();
    Z3Translator translator = new Z3Translator(ctx);

    for (SymbolicConstraint c : constraints) {
      solver.add(translator.translateConstraint(c));
    }
    Status status = solver.check();

    Model z3Model = null;
    Map<String, ModelValue> modelValues = Map.of();

    if (status == Status.SATISFIABLE) {
      z3Model = solver.getModel();
      modelValues = extractModel(z3Model, translator, ctx);
    }

    return new SolverResult(pathId, status, modelValues, ctx, z3Model);
  }

  private Map<String, ModelValue> extractModel(final Model model, final Z3Translator translator, final Context ctx) {
    Map<String, ModelValue> result = new HashMap<>();

    // Evaluate every expression the translator declared, by the exact name it used.
    // This covers: variables, virtual invokes, lengths, casts, field accesses.
    for (Map.Entry<String, Expr<?>> entry : translator.getDeclarations().entrySet()) {
      String name = entry.getKey();
      Expr<?> expr = entry.getValue();

      try {
        if (expr.getSort().getSortKind() == Z3_ARRAY_SORT) {
          // Store under the Z3 constant name (e.g. "arr"), not the cache key
          String modelKey = name.contains(":") ? name.substring(0, name.indexOf(':')) : name;
          System.out.println("extractModel array: name=" + name + " modelKey=" + modelKey);
          result.put(modelKey, new ModelValue.ArrayValue(
                  (ArrayExpr<IntSort, ?>) expr, model, ctx));
        } else {
          Expr<?> evaluated = model.eval(expr, true);
          result.put(name, ModelValue.fromExpr(evaluated, model, ctx));
        }
      } catch (IllegalStateException ignored) {
        // unsupported sort — skip
      }
    }

    // Also extract field functions (arity-1 decls named "field_*")
    for (FuncDecl<?> decl : model.getDecls()) {
      if (decl.getArity() != 1) continue;
      String declName = decl.getName().toString();
      if (!declName.startsWith("field_")) continue;

      String fieldName = declName.substring("field_".length());
      com.microsoft.z3.FuncInterp<?> interp = model.getFuncInterp(decl);
      if (interp == null) continue;

      for (com.microsoft.z3.FuncInterp.Entry<?> e : interp.getEntries()) {
        String baseArg = e.getArgs()[0].toString();
        String key = baseArg + "." + fieldName;
        try {
          result.put(key, ModelValue.fromExpr(e.getValue(), model, ctx));
        } catch (IllegalStateException ignored) {}
      }
    }

    return result;
  }
}
