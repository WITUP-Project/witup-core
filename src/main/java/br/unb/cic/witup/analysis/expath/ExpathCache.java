package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.expath.records.CallSite;
import br.unb.cic.witup.analysis.expath.records.CallSiteKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cache for intermediate artifacts used during expath computation.
 *
 * <p>This abstraction keeps {@link br.unb.cic.witup.analysis.expath.GlobalExpathBuilder} decoupled
 * from any specific cache backend. The in-memory implementation is suitable for unit tests and
 * small/medium projects; larger projects may benefit from an external KV store.
 */
public interface ExpathCache {

    Optional<List<List<ResolvedThrowCondition>>> getLocalResolved(String methodSig);
    void putLocalResolved(String methodSig, List<List<ResolvedThrowCondition>> paths);

    Optional<List<CallSite>> getCallSites(String callerMethodSig);
    void putCallSites(String callerMethodSig, List<CallSite> callSites);

    Optional<List<List<ResolvedThrowCondition>>> getBoundedCalleePaths(CallSiteKey key);
    void putBoundedCalleePaths(CallSiteKey key, List<List<ResolvedThrowCondition>> paths);

    Optional<Map<String, SymExpr>> getSubstitution(CallSiteKey key);
    void putSubstitution(CallSiteKey key, Map<String, SymExpr> subst);

    /**
     * Cache of callee "parameter index -> local name" mapping.
     *
     * <p>This mapping is stable per method and expensive to recompute if done per call site.
     */
    Optional<Map<Integer, String>> getParamIndexToLocal(String calleeMethodSig);
    void putParamIndexToLocal(String calleeMethodSig, Map<Integer, String> idxToLocal);
}

