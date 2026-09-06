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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
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
  public static final int TWENTY_SECONDS = 20000;
  static final int MAX_CONSTRAINT_DEPTH = 100;
  static final int PARALLEL_PATH_THRESHOLD = 1000;
  public static final int ONE_THOUSAND_TASKS = 1000;
  private final Map<String, MethodSummary> methodSummaries;
  private final Workspace mainWorkspace;

  // the method that receives method summaries needs to, for each set
  // of symbolic constraints, translate them to z3, and then solve
  public SymbolicConstraintSolver(final Map<String, MethodSummary> methodSummaries) {
    this.methodSummaries = methodSummaries;
    this.mainWorkspace = Workspace.make();
  }

  public Map<String, List<SolverResult>> solveMethods(final Map<String, String> failures) {
    Map<String, List<ExceptionPath>> pathsBySignature = new LinkedHashMap<>();
    for (MethodSummary summary : methodSummaries.values()) {
      pathsBySignature.put(summary.methodSignature(), summary.exceptionPaths());
    }
    return solveMethodPaths(pathsBySignature, failures);
  }

  /** Solves an set of paths per method signature. Composes callee's flows. */
  public Map<String, List<SolverResult>> solveMethodPaths(
      final Map<String, List<ExceptionPath>> pathsBySignature, final Map<String, String> failures) {
    Map<String, List<SolverResult>> methodSolutions = new HashMap<>();
    for (Map.Entry<String, List<ExceptionPath>> entry : pathsBySignature.entrySet()) {
      String sig = entry.getKey();
      try {
        List<ExceptionPath> paths = entry.getValue();
        List<SolverResult> results =
            paths.size() > PARALLEL_PATH_THRESHOLD
                ? solveInParallel(sig, paths)
                : solveSequentially(sig, paths);
        methodSolutions.put(sig, results);
      } catch (Exception e) {
        log.warn("Failed to solve {}: {}", sig, e.getMessage(), e);
        failures.put(sig, e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    }
    return methodSolutions;
  }

  private List<SolverResult> solveSequentially(final String sig, final List<ExceptionPath> paths) {
    List<SolverResult> results = new ArrayList<>(paths.size());
    for (int i = 0; i < paths.size(); i++) {
      log.debug("Solving path {}/{} for {}", i + 1, paths.size(), sig);
      results.add(checkPathOn(mainWorkspace, sig + "#" + i, paths.get(i).getConstraints()));
    }
    return results;
  }

  // Each worker thread holds its own Z3 Context+Solver+Translator (Z3 contexts are not
  // thread-safe, and a fresh-context-per-method approach measured 2.5x slower in earlier
  // experiments). The pool runs at 2x available cores; per-task exceptions degrade to a
  // MAYBE result rather than aborting the rest of the method.
  private List<SolverResult> solveInParallel(final String sig, final List<ExceptionPath> paths) {
    int parallelism = Runtime.getRuntime().availableProcessors() * 2;
    log.info("Solving {} paths for {} in parallel ({} workers)", paths.size(), sig, parallelism);

    AtomicInteger threadId = new AtomicInteger();
    ThreadFactory factory =
        r -> {
          Thread t = new Thread(r, "z3-worker-" + threadId.getAndIncrement());
          t.setDaemon(true);
          return t;
        };
    ExecutorService pool = Executors.newFixedThreadPool(parallelism, factory);
    List<Workspace> workspaces = new CopyOnWriteArrayList<>();
    ThreadLocal<Workspace> tlWorkspace =
        ThreadLocal.withInitial(
            () -> {
              Workspace w = Workspace.make();
              workspaces.add(w);
              return w;
            });

    AtomicInteger done = new AtomicInteger();
    int total = paths.size();
    List<Callable<SolverResult>> tasks = new ArrayList<>(total);
    for (int i = 0; i < total; i++) {
      final int idx = i;
      final String pathId = sig + "#" + idx;
      tasks.add(
          () -> {
            try {
              SolverResult r =
                  checkPathOn(tlWorkspace.get(), pathId, paths.get(idx).getConstraints());
              int d = done.incrementAndGet();
              if (d % ONE_THOUSAND_TASKS == 0 || d == total) {
                log.info("Solved {}/{} paths for {}", d, total, sig);
              }
              return r;
            } catch (RuntimeException e) {
              log.warn("Path {} failed: {}", pathId, e.getMessage());
              return new SolverResult(pathId, SolverStatus.MAYBE, Map.of());
            }
          });
    }

    try {
      List<Future<SolverResult>> futures = pool.invokeAll(tasks);
      List<SolverResult> results = new ArrayList<>(futures.size());
      for (int i = 0; i < futures.size(); i++) {
        try {
          results.add(futures.get(i).get());
        } catch (ExecutionException e) {
          // Callable already catches RuntimeExceptions; this is the defensive fallback for
          // anything that slipped through (e.g. an Error). Surface as MAYBE so the per-method
          // result list stays index-aligned with paths.
          String pathId = sig + "#" + i;
          log.warn("Task {} failed: {}", pathId, e.getCause().getMessage());
          results.add(new SolverResult(pathId, SolverStatus.MAYBE, Map.of()));
        }
      }
      return results;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Parallel solving interrupted for " + sig, e);
    } finally {
      pool.shutdown();
      for (Workspace w : workspaces) {
        try {
          w.close();
        } catch (RuntimeException closeEx) {
          log.warn("Failed to close worker Z3 context: {}", closeEx.getMessage());
        }
      }
    }
  }

  public SolverResult checkPath(final String pathId, final List<SymbolicConstraint> constraints) {
    return checkPathOn(mainWorkspace, pathId, constraints);
  }

  private SolverResult checkPathOn(
      final Workspace ws, final String pathId, final List<SymbolicConstraint> constraints) {
    Solver solver = ws.solver();
    Z3Translator translator = ws.translator();
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
    Map<String, ModelValue> modelValueMap = isSat ? extractModel(ws, model) : Map.of();

    solver.pop();

    SolverStatus solverStatus = skipped > 0 ? SolverStatus.MAYBE : SolverStatus.fromZ3(status);
    return new SolverResult(pathId, solverStatus, modelValueMap);
  }

  private Map<String, ModelValue> extractModel(final Workspace ws, final Model model) {
    Map<String, ModelValue> modelValueMap = new HashMap<>();

    // should cover variables, virtual invokes, lengths, casts, field accesses
    // when they are all implemented
    extractDeclarations(ws, model, modelValueMap);
    // Also extract field functions (arity-1 decls named "field_*")
    extractFieldFunctions(model, ws.ctx(), modelValueMap);

    return modelValueMap;
  }

  private void extractDeclarations(
      final Workspace ws, final Model model, final Map<String, ModelValue> modelValueMap) {
    Map<String, String> descriptions = ws.translator().getIdDescriptions();
    for (Map.Entry<String, Expr<?>> entry : ws.translator().getDeclarations().entrySet()) {
      String id = entry.getKey();
      Expr<?> expr = entry.getValue();
      String modelKey = descriptions.getOrDefault(id, id);
      try {
        if (expr.getSort().getSortKind() == Z3_ARRAY_SORT) {
          modelValueMap.put(
              toModelKey(modelKey), new ArrayValue((ArrayExpr<IntSort, ?>) expr, model, ws.ctx()));
        } else {
          Expr<?> evaluated = model.eval(expr, true);
          modelValueMap.put(modelKey, ModelValue.fromExpr(evaluated, model, ws.ctx()));
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

  // Bundles the per-thread Z3 stack so the parallel path can hand each worker its own
  // Context/Solver/Translator without leaking those types into the outer control flow.
  private record Workspace(Context ctx, Solver solver, Z3Translator translator) {
    static Workspace make() {
      Context c = new Context();
      Solver s = c.mkSolver();
      Params p = c.mkParams();
      p.add("timeout", TWENTY_SECONDS);
      s.setParameters(p);
      return new Workspace(c, s, new Z3Translator(c));
    }

    void close() {
      ctx.close();
    }
  }
}
