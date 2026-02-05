package br.unb.cic.witup.analysis.expath.cache;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CachingGlobalExpathDecorator implements GlobalExpathPort {
    private final GlobalExpathPort delegate;
    private final Map<Key, List<List<ResolvedThrowCondition>>> cache = new ConcurrentHashMap<>();

    public CachingGlobalExpathDecorator(GlobalExpathPort delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<List<ResolvedThrowCondition>> compose(
            Map<String, SootUpPropertyGraphs> graphsBySignature,
            String rootSignature,
            int maxDepth) {
        Key key = new Key(rootSignature, maxDepth, fingerprint(graphsBySignature));
        return cache.computeIfAbsent(key, k -> delegate.compose(graphsBySignature, rootSignature, maxDepth));
    }

    private static String fingerprint(Map<String, SootUpPropertyGraphs> graphsBySignature) {
        return graphsBySignature.keySet().stream().sorted().reduce("", (a, b) -> a + "|" + b);
    }


    private record Key(String rootSignature, int maxDepth, String fp) {}
}
