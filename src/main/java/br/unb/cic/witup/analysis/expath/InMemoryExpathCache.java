package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.expath.records.CallSite;
import br.unb.cic.witup.analysis.expath.records.CallSiteKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryExpathCache implements ExpathCache {

    private final ConcurrentHashMap<String, List<List<ResolvedThrowCondition>>> localResolved =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<CallSite>> callSites =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<CallSiteKey, Map<String, SymExpr>> substitutions =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<CallSiteKey, List<List<ResolvedThrowCondition>>> boundedCalleePaths =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Map<Integer, String>> paramIdxToLocal =
            new ConcurrentHashMap<>();

    @Override
    public Optional<List<List<ResolvedThrowCondition>>> getLocalResolved(String methodSig) {
        return Optional.ofNullable(localResolved.get(methodSig));
    }

    @Override
    public void putLocalResolved(String methodSig, List<List<ResolvedThrowCondition>> paths) {
        localResolved.put(methodSig, paths);
    }

    @Override
    public Optional<List<CallSite>> getCallSites(String callerMethodSig) {
        return Optional.ofNullable(callSites.get(callerMethodSig));
    }

    @Override
    public void putCallSites(String callerMethodSig, List<CallSite> callSitesList) {
        callSites.put(callerMethodSig, callSitesList);
    }

    @Override
    public Optional<List<List<ResolvedThrowCondition>>> getBoundedCalleePaths(CallSiteKey key) {
        return Optional.ofNullable(boundedCalleePaths.get(key));
    }

    @Override
    public void putBoundedCalleePaths(CallSiteKey key, List<List<ResolvedThrowCondition>> paths) {
        boundedCalleePaths.put(key, paths);
    }

    @Override
    public Optional<Map<String, SymExpr>> getSubstitution(CallSiteKey key) {
        return Optional.ofNullable(substitutions.get(key));
    }

    @Override
    public void putSubstitution(CallSiteKey key, Map<String, SymExpr> subst) {
        substitutions.put(key, subst);
    }

    @Override
    public Optional<Map<Integer, String>> getParamIndexToLocal(String calleeMethodSig) {
        return Optional.ofNullable(paramIdxToLocal.get(calleeMethodSig));
    }

    @Override
    public void putParamIndexToLocal(String calleeMethodSig, Map<Integer, String> idxToLocal) {
        paramIdxToLocal.put(calleeMethodSig, idxToLocal);
    }
}
