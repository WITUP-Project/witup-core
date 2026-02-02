package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.analysis.expath.records.CallSite;
import br.unb.cic.witup.analysis.expath.records.CallSiteKey;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds global exception paths by inlining (composing) local expaths across call sites.
 *
 * <p>Global expath = prefix (caller entry -> call) + localExpath(callee)
 * with parameter-binding substitution applied at the callsite.
 */
public final class GlobalExpathBuilder {

    private final LocalExpathBuilder localBuilder;
    private final CallSiteFinder callSiteFinder;
    private final ParameterBinder parameterBinder;
    private final ExpathCache cache;
    private final ExpathOptions options;

    public GlobalExpathBuilder(
            final LocalExpathBuilder localBuilder,
            final CallSiteFinder callSiteFinder,
            final ParameterBinder parameterBinder) {
        this(localBuilder, callSiteFinder, parameterBinder, new InMemoryExpathCache(), ExpathOptions.DEFAULT);
    }

    public GlobalExpathBuilder(
            final LocalExpathBuilder localBuilder,
            final CallSiteFinder callSiteFinder,
            final ParameterBinder parameterBinder,
            final ExpathCache cache,
            final ExpathOptions options) {
        this.localBuilder = Objects.requireNonNull(localBuilder);
        this.callSiteFinder = Objects.requireNonNull(callSiteFinder);
        this.parameterBinder = Objects.requireNonNull(parameterBinder);
        this.cache = Objects.requireNonNull(cache);
        this.options = Objects.requireNonNull(options);
    }

    public List<List<ResolvedThrowCondition>> buildGlobalResolvedPaths(
            final Map<String, SootUpPropertyGraphs> graphsBySignature, final String rootSignature) {

        SootUpPropertyGraphs rootGraphs = graphsBySignature.get(rootSignature);
        if (rootGraphs == null) {
            throw new IllegalArgumentException("No graphs for root method: " + rootSignature);
        }

        final List<List<ResolvedThrowCondition>> out = new ArrayList<>();
        final ResolvedExpathTrie trie = options.useTrieCompression() ? new ResolvedExpathTrie() : null;

        // (A) throws locais no root (cacheable)
        List<List<ResolvedThrowCondition>> rootLocal = getOrComputeLocalResolved(rootSignature, rootGraphs);
        if (trie == null) {
            out.addAll(rootLocal);
        } else {
            for (List<ResolvedThrowCondition> p : rootLocal) {
                trie.insert(p);
            }
        }

        // (B) inlining interprocedural
        WITUpGraph rootCfg = WITUpGraph.fromPropertyGraph(rootGraphs.getCFG());
        WITUpGraph rootDdg = WITUpGraph.fromPropertyGraph(rootGraphs.getDDG());
        Resolver rootResolver = new Resolver(rootDdg);

        List<CallSite> callSites = getOrComputeCallSites(rootSignature, rootCfg);

        for (CallSite site : callSites) {
            String calleeSig = site.calleeSignature();
            SootUpPropertyGraphs calleeGraphs = graphsBySignature.get(calleeSig);
            if (calleeGraphs == null) continue;

            // prefix: entry(root) -> callNode
            List<List<ThrowCondition>> prefixPaths =
                    ConditionPathFinder.findConditionPathsToNode(rootCfg, site.callNode(), options);

            List<List<ResolvedThrowCondition>> prefixResolved =
                    rootResolver.resolveConditionPaths(prefixPaths, rootDdg);

            // callee local expaths (cacheable by calleeSig)
            List<List<ResolvedThrowCondition>> calleeLocal =
                    getOrComputeLocalResolved(calleeSig, calleeGraphs);

            CallSiteKey key =
                    new CallSiteKey(
                            rootSignature,
                            calleeSig,
                            stableCallNodeId(site),
                            site.args());

            // substitution (callee locals -> caller args), cacheable by callsite
            Map<String, SymExpr> subst = getOrComputeSubstitution(key, calleeSig, calleeGraphs, site.args());

            // callee paths already substituted for this callsite, cacheable by callsite
            List<List<ResolvedThrowCondition>> boundedCallee =
                    getOrComputeBoundedCalleePaths(key, calleeLocal, subst);

            // Compose: prefix(root->call) + bounded(callee throw paths)
            for (List<ResolvedThrowCondition> prefix : prefixResolved) {
                for (List<ResolvedThrowCondition> calleeBoundedPath : boundedCallee) {
                    List<ResolvedThrowCondition> global =
                            new ArrayList<>(prefix.size() + calleeBoundedPath.size());
                    global.addAll(prefix);
                    global.addAll(calleeBoundedPath);

                    if (trie == null) {
                        out.add(global);
                    } else {
                        trie.insert(global);
                    }
                }
            }
        }

        return trie == null ? out : trie.materialize();
    }

    // ============================================================
    // Cache helpers (delegated to ExpathCache)
    // ============================================================

    private List<List<ResolvedThrowCondition>> getOrComputeLocalResolved(
            final String methodSig, final SootUpPropertyGraphs graphs) {

        return cache.getLocalResolved(methodSig)
                .orElseGet(
                        () -> {
                            List<List<ResolvedThrowCondition>> computed = localBuilder.buildLocalResolvedPaths(graphs);
                            cache.putLocalResolved(methodSig, computed);
                            return computed;
                        });
    }

    private List<CallSite> getOrComputeCallSites(final String callerSig, final WITUpGraph callerCfg) {

        return cache.getCallSites(callerSig)
                .orElseGet(
                        () -> {
                            List<CallSite> sites = callSiteFinder.findCallSites(callerCfg);
                            cache.putCallSites(callerSig, sites);
                            return sites;
                        });
    }

    private Map<String, SymExpr> getOrComputeSubstitution(
            final CallSiteKey key,
            final String calleeSig,
            final SootUpPropertyGraphs calleeGraphs,
            final List<String> args) {

        return cache.getSubstitution(key)
                .orElseGet(
                        () -> {
                            Map<String, SymExpr> subst =
                                    parameterBinder.bindCalleeParameterLocalsToArgs(calleeSig, calleeGraphs, args);
                            cache.putSubstitution(key, subst);
                            return subst;
                        });
    }

    private List<List<ResolvedThrowCondition>> getOrComputeBoundedCalleePaths(
            final CallSiteKey key,
            final List<List<ResolvedThrowCondition>> calleeLocal,
            final Map<String, SymExpr> subst) {

        return cache.getBoundedCalleePaths(key)
                .orElseGet(
                        () -> {
                            List<List<ResolvedThrowCondition>> bounded = new ArrayList<>(calleeLocal.size());
                            for (List<ResolvedThrowCondition> p : calleeLocal) {
                                bounded.add(substituteAll(p, subst));
                            }
                            cache.putBoundedCalleePaths(key, bounded);
                            return bounded;
                        });
    }

    private static String stableCallNodeId(final CallSite site) {
        Object node = site.callNode() == null ? null : site.callNode().getNode();
        String s = String.valueOf(node);
        return Integer.toHexString(s.hashCode());
    }

    // ============================================================
    // Substitution
    // ============================================================

    private static List<ResolvedThrowCondition> substituteAll(
            final List<ResolvedThrowCondition> path, final Map<String, SymExpr> subst) {

        List<ResolvedThrowCondition> out = new ArrayList<>(path.size());
        for (ResolvedThrowCondition rtc : path) {
            SymExpr expr = rtc.getNode();
            for (Map.Entry<String, SymExpr> e : subst.entrySet()) {
                expr = expr.substitute(e.getKey(), e.getValue());
            }
            out.add(new ResolvedThrowCondition(expr, rtc.isTruthValue()));
        }
        return out;
    }
}
