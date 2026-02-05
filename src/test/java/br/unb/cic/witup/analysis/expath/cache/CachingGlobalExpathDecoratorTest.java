package br.unb.cic.witup.analysis.expath.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymVar;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class CachingGlobalExpathDecoratorTest {

    @Test
    void shouldCallDelegateOnlyOnceForSameKey() {
        AtomicInteger calls = new AtomicInteger(0);

        GlobalExpathPort fakeDelegate = (graphs, root, depth) -> {
            calls.incrementAndGet();
            return List.of(List.of(new ResolvedThrowCondition(new SymVar("x"), true)));
        };

        CachingGlobalExpathDecorator cached = new CachingGlobalExpathDecorator(fakeDelegate);

        Map<String, SootUpPropertyGraphs> graphs = new HashMap<>();
        String root = "<A: void m()>";
        int depth = 2;

        List<List<ResolvedThrowCondition>> first = cached.compose(graphs, root, depth);
        List<List<ResolvedThrowCondition>> second = cached.compose(graphs, root, depth);

        assertEquals(1, calls.get(), "Delegate should be called only once for same cache key.");
        assertSame(first, second, "Expected same cached instance on second call.");
    }

    @Test
    void shouldMissCacheWhenDepthChanges() {
        AtomicInteger calls = new AtomicInteger(0);

        GlobalExpathPort fakeDelegate = (graphs, root, depth) -> {
            calls.incrementAndGet();
            return List.of(List.of(new ResolvedThrowCondition(new SymVar("d" + depth), true)));
        };

        CachingGlobalExpathDecorator cached = new CachingGlobalExpathDecorator(fakeDelegate);

        Map<String, SootUpPropertyGraphs> graphs = new HashMap<>();
        String root = "<A: void m()>";

        cached.compose(graphs, root, 1);
        cached.compose(graphs, root, 2);

        assertEquals(2, calls.get(), "Different depth should generate different cache key.");
    }

    @Test
    void shouldMissCacheWhenRootChanges() {
        AtomicInteger calls = new AtomicInteger(0);

        GlobalExpathPort fakeDelegate = (graphs, root, depth) -> {
            calls.incrementAndGet();
            return List.of(List.of(new ResolvedThrowCondition(new SymVar(root), true)));
        };

        CachingGlobalExpathDecorator cached = new CachingGlobalExpathDecorator(fakeDelegate);

        Map<String, SootUpPropertyGraphs> graphs = new HashMap<>();

        cached.compose(graphs, "<A: void m()>", 2);
        cached.compose(graphs, "<B: void m()>", 2);

        assertEquals(2, calls.get(), "Different root signature should generate different cache key.");
    }

    @Test
    void shouldMissCacheWhenGraphFingerprintChanges() {
        AtomicInteger calls = new AtomicInteger(0);

        GlobalExpathPort fakeDelegate = (graphs, root, depth) -> {
            calls.incrementAndGet();
            return List.of(List.of(new ResolvedThrowCondition(new SymVar("ok"), true)));
        };

        CachingGlobalExpathDecorator cached = new CachingGlobalExpathDecorator(fakeDelegate);

        // HashMap aceita null value; Map.of NÃO aceita.
        Map<String, SootUpPropertyGraphs> g1 = new HashMap<>();
        g1.put("m1", null);

        Map<String, SootUpPropertyGraphs> g2 = new HashMap<>();
        g2.put("m1", null);
        g2.put("m2", null);

        cached.compose(g1, "<A: void m()>", 2);
        cached.compose(g2, "<A: void m()>", 2);

        assertEquals(2, calls.get(), "Different graph fingerprint should miss cache.");
    }

    @Test
    void shouldLogPerformanceDifferenceWithAndWithoutCache() {
        GlobalExpathPort slowDelegate = (graphs, root, depth) -> {
            busyWork(4_000_000); // carga sintética
            return List.of(List.of(new ResolvedThrowCondition(new SymVar("x"), true)));
        };

        GlobalExpathPort withoutCache = slowDelegate;
        GlobalExpathPort withCache = new CachingGlobalExpathDecorator(slowDelegate);

        Map<String, SootUpPropertyGraphs> graphs = new HashMap<>();
        graphs.put("m1", null);
        graphs.put("m2", null);

        String root = "<A: void m()>";
        int depth = 2;
        int rounds = 12;

        // warm-up JIT
        for (int i = 0; i < 5; i++) {
            withoutCache.compose(graphs, root, depth);
            withCache.compose(graphs, root, depth);
        }

        // Mede sem cache (todas chamadas custosas)
        List<Long> noCacheTimes = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            long t = measureNanos(() -> withoutCache.compose(graphs, root, depth));
            noCacheTimes.add(t);
        }

        // Mede com cache:
        // 1ª chamada pode ser miss, as demais devem ser hit
        List<Long> withCacheTimes = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            long t = measureNanos(() -> withCache.compose(graphs, root, depth));
            withCacheTimes.add(t);
        }

        double noCacheAvgMs = avgMillis(noCacheTimes);
        double withCacheAvgMs = avgMillis(withCacheTimes);

        // média de hits puros (descarta primeira chamada com-cache)
        double withCacheHitAvgMs = avgMillis(withCacheTimes.subList(1, withCacheTimes.size()));

        double speedupAvg = noCacheAvgMs / Math.max(withCacheAvgMs, 0.000001d);
        double speedupHit = noCacheAvgMs / Math.max(withCacheHitAvgMs, 0.000001d);

        System.out.println("\n=== Cache Performance Comparison ===");
        System.out.println("rounds: " + rounds);
        System.out.printf("no-cache avg       : %.3f ms%n", noCacheAvgMs);
        System.out.printf("with-cache avg     : %.3f ms%n", withCacheAvgMs);
        System.out.printf("with-cache hit avg : %.3f ms%n", withCacheHitAvgMs);
        System.out.printf("speedup avg        : %.2fx%n", speedupAvg);
        System.out.printf("speedup hit-only   : %.2fx%n", speedupHit);
        System.out.println("no-cache nanos     : " + noCacheTimes);
        System.out.println("with-cache nanos   : " + withCacheTimes);

        // assert conservador para CI:
        assertTrue(withCacheHitAvgMs < noCacheAvgMs,
                "Expected cached hit average to be faster than no-cache average.");
    }

    private static long measureNanos(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return System.nanoTime() - start;
    }

    private static double avgMillis(List<Long> nanos) {
        long sum = 0L;
        for (Long n : nanos) {
            sum += n;
        }
        return (sum / (double) nanos.size()) / 1_000_000.0;
    }

    private static void busyWork(int iterations) {
        long acc = 0L;
        for (int i = 0; i < iterations; i++) {
            acc += ((long) i * 31L) ^ (i >>> 3);
        }
        if (acc == Long.MIN_VALUE) {
            System.out.println("unreachable");
        }
    }
}
