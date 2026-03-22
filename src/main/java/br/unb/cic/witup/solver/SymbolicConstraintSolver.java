package br.unb.cic.witup.solver;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_ARRAY_SORT;

import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.ModelValue;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.FuncInterp;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates SymbolicConstraints into Z3, evaluates and returns a model if the constraints are
 * satisfiable.
 */
public final class SymbolicConstraintSolver {
  private static final Logger log = LoggerFactory.getLogger("SymbolicConstraintSolver");

  // need to extract constants shared across this layer.
  public static final String FIELD_FUNC_PREFIX = "field_";
  public static final String IS_NULL = "_is_null";
  public static final int TWENTY_SECONDS = 20000;
  private Map<String, MethodSummary> methodSummaries = new HashMap<>();
  private final Context ctx = new Context();
  private final Solver solver = ctx.mkSolver();

  public SymbolicConstraintSolver(final List<List<SymbolicConstraint>> symbolicConstraintPaths) {}

  // the method that receives method summaries needs to, for each set
  // of symbolic constraints, translate them to z3, solve
  // we produce MethodSolutions, that map method name to solutions
  public SymbolicConstraintSolver(final Map<String, MethodSummary> methodSummaries) {
    this.methodSummaries = methodSummaries;
    Params params = ctx.mkParams();
    params.add("timeout", TWENTY_SECONDS);
    solver.setParameters(params);
  }

  public Map<String, List<SolverResult>> solveConstraintsSafe(final Map<String, String> failures) {
    Map<String, List<SolverResult>> methodSolutions = new HashMap<>();
    for (MethodSummary summary : methodSummaries.values()) {
      String sig = summary.getMethodSignature();
      try {
        List<SolverResult> results = new ArrayList<>();
        List<List<SymbolicConstraint>> paths = summary.getSymbolicConstraintPaths();
        for (int i = 0; i < paths.size(); i++) {
          log.debug("Solving path {}/{} for {}", i + 1, paths.size(), sig);
          results.add(checkPath(sig + "#" + i, paths.get(i)));
        }
        methodSolutions.put(sig, results);
      } catch (Exception e) {
        log.warn("Failed to solve {}: {}", sig, e.getMessage(), e);
        failures.put(sig, e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    }
    return methodSolutions;
  }

  public SolverResult checkPath(final String pathId, final List<SymbolicConstraint> constraints) {
    solver.push();
    Z3Translator translator = new Z3Translator(ctx);

    for (SymbolicConstraint c : constraints) {
      solver.add(translator.translateConstraint(c));
    }

    Status status = solver.check();
    boolean isSat = status == Status.SATISFIABLE;

    Model model = isSat ? solver.getModel() : null;
    Map<String, ModelValue> modelValueMap = isSat ? extractModel(model, translator) : Map.of();

    solver.pop();

    return new SolverResult(pathId, status, modelValueMap);
  }

  private Map<String, ModelValue> extractModel(final Model model, final Z3Translator translator) {
    Map<String, ModelValue> modelValueMap = new HashMap<>();

    // should cover variables, virtual invokes, lengths, casts, field accesses
    // when they are all implemented
    extractDeclarations(model, translator, modelValueMap);
    // Also extract field functions (arity-1 decls named "field_*")
    extractFieldFunctions(model, ctx, modelValueMap);

    return modelValueMap;
  }

  private void extractDeclarations(
      final Model model,
      final Z3Translator translator,
      final Map<String, ModelValue> modelValueMap) {
    for (Map.Entry<String, Expr<?>> entry : translator.getDeclarations().entrySet()) {
      String name = entry.getKey();
      Expr<?> expr = entry.getValue();

      try {
        if (expr.getSort().getSortKind() == Z3_ARRAY_SORT) {
          // Store under the Z3 constant name (e.g. "arr"), not the cache key
          // very hacky and implemented like this after too much time debugging
          String modelKey = this.toModelKey(name);
          modelValueMap.put(modelKey, new ArrayValue((ArrayExpr<IntSort, ?>) expr, model, ctx));
        } else {
          Expr<?> evaluated = model.eval(expr, true);
          modelValueMap.put(name, ModelValue.fromExpr(evaluated, model, ctx));
        }
      } catch (IllegalStateException ignored) {
        // unsupported sort — skip for now. maybe throw to force correct implementation
      }
    }
  }

  private static void extractFieldFunctions(
      final Model model, final Context ctx, final Map<String, ModelValue> modelValueMap) {
    for (FuncDecl<?> decl : model.getDecls()) {
      if (decl.getArity() != 1) {
        continue;
      }
      String declName = decl.getName().toString();
      // need to strengthen this contract
      if (!declName.startsWith(FIELD_FUNC_PREFIX)) {
        continue;
      }

      String fieldName = declName.substring(FIELD_FUNC_PREFIX.length());
      FuncInterp<?> funcInterp = model.getFuncInterp(decl);
      if (funcInterp == null) {
        continue;
      }

      for (FuncInterp.Entry<?> e : funcInterp.getEntries()) {
        String baseArg = e.getArgs()[0].toString();
        String key = baseArg + "." + fieldName;
        try {
          modelValueMap.put(key, ModelValue.fromExpr(e.getValue(), model, ctx));
        } catch (IllegalStateException ignored) {
        }
      }
    }
  }

  private String toModelKey(final String name) {
    return name.contains(":") ? name.substring(0, name.indexOf(':')) : name;
  }
}
