package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.MethodCallSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.symbolic.SymExprResolver;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import br.unb.cic.witup.analysis.symbolic.expr.SymParamRef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import sootup.core.jimple.basic.Immediate;

// Discovers the transitive observable exception flow of a method by composing local
// per-method MethodSummary entries with the call graph at query time. Each method's
// MethodSummary stays local (own throws only); composition happens here, on demand.
//
// For a method M:
//   1. Start with M's own throws (DIRECT_ATHROW / RETHROW / IMPLICIT).
//   2. For each call site in M's WITUpGraph, recurse into the callee's observable
//      flow, filter through this site's in-scope catch handlers, prepend the callee
//      signature to provenance, substitute formals→actuals, and fold in.
//
// Memoized per-method across one walker instance so deep / diamond call graphs don't
// recompute callee flows per caller. Cycles short-circuit via in-progress tracking
// (under-approximates SCCs; sufficient for non-recursive call graphs).
public final class ExceptionFlowWalker {
  private final Map<String, MethodSummary> summaries;
  private final GraphRepository graphRepository;
  private final Map<String, List<ExceptionPath>> cache = new HashMap<>();
  private final Set<String> inProgress = new HashSet<>();

  public ExceptionFlowWalker(
      final Map<String, MethodSummary> summaries, final GraphRepository graphRepository) {
    this.summaries = summaries;
    this.graphRepository = graphRepository;
  }

  public List<ExceptionPath> observablePaths(final String methodSig) {
    List<ExceptionPath> cached = cache.get(methodSig);
    if (cached != null) {
      return cached;
    }
    if (!inProgress.add(methodSig)) {
      return List.of();
    }
    List<ExceptionPath> result = compute(methodSig);
    inProgress.remove(methodSig);
    cache.put(methodSig, result);
    return result;
  }

  private List<ExceptionPath> compute(final String methodSig) {
    MethodSummary summary = summaries.get(methodSig);
    if (summary == null) {
      return List.of();
    }

    List<ExceptionPath> result = new ArrayList<>();
    if (summary.exceptionPaths() != null) {
      result.addAll(summary.exceptionPaths());
    }

    Optional<WITUpGraph> graphOpt = graphRepository.getGraph(methodSig);
    if (graphOpt.isEmpty()) {
      return result;
    }
    WITUpGraph cpg = graphOpt.get();

    for (MethodCallSite site : cpg.getCallSites()) {
      MethodSummary calleeSummary = summaries.get(site.calleeSignature());
      if (calleeSummary == null) {
        continue;
      }
      List<SymParamRef> formals = calleeSummary.formalParams();
      if (formals == null || formals.size() != site.actuals().size()) {
        continue;
      }
      List<SymExpr> actuals = new ArrayList<>(site.actuals().size());
      for (Immediate imm : site.actuals()) {
        actuals.add(SymExprResolver.resolveLocalAt(SymExpr.fromJimple(imm), site.node(), cpg));
      }
      Set<String> caughtTypes = cpg.inScopeCatchTypes(site.node());
      List<ExceptionPath> calleeFlows = observablePaths(site.calleeSignature());

      for (ExceptionPath calleeFlow : calleeFlows) {
        if (isCaughtByAny(calleeFlow.getExceptionQualifiedName(), caughtTypes)) {
          continue;
        }
        List<SymbolicConstraint> substituted =
            new ArrayList<>(calleeFlow.getConstraints().size());
        for (SymbolicConstraint c : calleeFlow.getConstraints()) {
          SymExpr expr = c.symExpr();
          for (int i = 0; i < formals.size(); i++) {
            expr = expr.substituteParam(formals.get(i).getIndex(), actuals.get(i));
          }
          substituted.add(new SymbolicConstraint(expr, c.truthValue()));
        }
        List<String> provenance = new ArrayList<>(calleeFlow.getProvenance().size() + 1);
        provenance.add(site.calleeSignature());
        provenance.addAll(calleeFlow.getProvenance());
        result.add(
            new ExceptionPath(
                substituted,
                site.node(),
                calleeFlow.getExceptionQualifiedName(),
                ThrowSiteKind.CALLEE_PROPAGATED,
                provenance));
      }
    }
    return result;
  }

  // Pragmatic catch-type matcher: exact match + recognise the three catch-all aliases
  // at the top of the JDK exception hierarchy. Real subtype matching across user-defined
  // hierarchies is deferred to a SootUp TypeHierarchy integration (see deferred work).
  private static boolean isCaughtByAny(final String thrownType, final Set<String> caughtTypes) {
    if (thrownType == null || caughtTypes.isEmpty()) {
      return false;
    }
    for (String caught : caughtTypes) {
      if (thrownType.equals(caught)) {
        return true;
      }
      if ("java.lang.Throwable".equals(caught)) {
        return true;
      }
      if ("java.lang.Exception".equals(caught) && !isErrorType(thrownType)) {
        return true;
      }
      if ("java.lang.RuntimeException".equals(caught) && isUncheckedType(thrownType)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isErrorType(final String type) {
    return "java.lang.Error".equals(type)
        || "java.lang.OutOfMemoryError".equals(type)
        || "java.lang.StackOverflowError".equals(type)
        || "java.lang.AssertionError".equals(type);
  }

  private static boolean isUncheckedType(final String type) {
    return "java.lang.RuntimeException".equals(type)
        || "java.lang.NullPointerException".equals(type)
        || "java.lang.IllegalArgumentException".equals(type)
        || "java.lang.IllegalStateException".equals(type)
        || "java.lang.ArrayIndexOutOfBoundsException".equals(type)
        || "java.lang.IndexOutOfBoundsException".equals(type)
        || "java.lang.ArithmeticException".equals(type)
        || "java.lang.NegativeArraySizeException".equals(type)
        || "java.lang.ClassCastException".equals(type)
        || "java.lang.UnsupportedOperationException".equals(type);
  }
}
