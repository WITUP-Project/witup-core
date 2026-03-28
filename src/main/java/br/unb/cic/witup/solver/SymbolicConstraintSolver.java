package br.unb.cic.witup.solver;

import static com.microsoft.z3.enumerations.Z3_sort_kind.Z3_ARRAY_SORT;

import br.unb.cic.witup.analysis.ExceptionPath;
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
  static final int MAX_CONSTRAINT_DEPTH = 100;
  private final Map<String, MethodSummary> methodSummaries;
  private final Context ctx = new Context();
  private final Solver solver = ctx.mkSolver();
  private final Z3Translator translator = new Z3Translator(ctx);

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
        List<ExceptionPath> paths = summary.getExceptionPaths();
        for (int i = 0; i < paths.size(); i++) {
          log.debug("Solving path {}/{} for {}", i + 1, paths.size(), sig);
          results.add(checkPath(sig + "#" + i, paths.get(i).getConstraints()));
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
    translator.resetForNewPath();

    int skipped = 0;
    for (int i = 0; i < constraints.size(); i++) {
      SymbolicConstraint c = constraints.get(i);
      int d = c.symExpr().depth();
      if (d > MAX_CONSTRAINT_DEPTH) {
        log.info(
            "Skipping constraint {}/{} for {} — depth {} exceeds bound",
            i + 1,
            constraints.size(),
            pathId,
            d);
        skipped++;
        continue;
      }
      try {
        solver.add(translator.translateConstraint(c));
      } catch (Exception e) {
        log.error("Z3 error adding constraint for {}: {}", pathId, e.getMessage());
        log.error("Stack trace: ", e);
        throw e;
      }
    }

    Status status = solver.check();
    boolean isSat = status == Status.SATISFIABLE;

    Model model = isSat ? solver.getModel() : null;
    Map<String, ModelValue> modelValueMap = isSat ? extractModel(model) : Map.of();

    solver.pop();

    SolverStatus solverStatus = skipped > 0 ? SolverStatus.MAYBE : SolverStatus.fromZ3(status);
    return new SolverResult(pathId, solverStatus, modelValueMap);
  }

  private Map<String, ModelValue> extractModel(final Model model) {
    Map<String, ModelValue> modelValueMap = new HashMap<>();

    // should cover variables, virtual invokes, lengths, casts, field accesses
    // when they are all implemented
    extractDeclarations(model, modelValueMap);
    // Also extract field functions (arity-1 decls named "field_*")
    extractFieldFunctions(model, ctx, modelValueMap);

    return modelValueMap;
  }

  private void extractDeclarations(
      final Model model,
      final Map<String, ModelValue> modelValueMap) {
    Map<String, String> descriptions = translator.getIdDescriptions();
    for (Map.Entry<String, Expr<?>> entry : translator.getDeclarations().entrySet()) {
      String id = entry.getKey();
      Expr<?> expr = entry.getValue();
      String modelKey = descriptions.getOrDefault(id, id);
      try {
        if (expr.getSort().getSortKind() == Z3_ARRAY_SORT) {
          modelValueMap.put(
              toModelKey(modelKey), new ArrayValue((ArrayExpr<IntSort, ?>) expr, model, ctx));
        } else {
          Expr<?> evaluated = model.eval(expr, true);
          modelValueMap.put(modelKey, ModelValue.fromExpr(evaluated, model, ctx));
        }
      } catch (IllegalStateException ignored) {
        log.info("extractDeclarations failed for {} in expr {}", id, expr);
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
