package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymExpr;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prefix-sharing structure (TRIE) for sets of resolved exception paths.
 *
 * <p>This reduces duplication when many paths share long common prefixes (common in global expaths
 * after inlining). Materialization back to {@code List<List<ResolvedThrowCondition>>} is supported
 * for compatibility with the rest of the code and tests.
 */
final class ResolvedExpathTrie {

    private final TrieNode root = new TrieNode();

    void insert(final List<ResolvedThrowCondition> path) {
        TrieNode cur = root;
        for (ResolvedThrowCondition c : path) {
            EdgeKey key = EdgeKey.from(c);
            Edge edge = cur.children.get(key);
            if (edge == null) {
                edge = new Edge(c, new TrieNode());
                cur.children.put(key, edge);
            }
            cur = edge.child;
        }
        cur.terminal = true;
    }

    List<List<ResolvedThrowCondition>> materialize() {
        List<List<ResolvedThrowCondition>> out = new ArrayList<>();
        dfs(root, new ArrayList<>(), out);
        return out;
    }

    private static void dfs(TrieNode node, List<ResolvedThrowCondition> prefix, List<List<ResolvedThrowCondition>> out) {
        if (node.terminal) {
            out.add(new ArrayList<>(prefix));
        }
        for (Edge e : node.children.values()) {
            prefix.add(e.condition);
            dfs(e.child, prefix, out);
            prefix.remove(prefix.size() - 1);
        }
    }

    private static final class TrieNode {
        private final Map<EdgeKey, Edge> children = new LinkedHashMap<>();
        private boolean terminal = false;
    }

    private record Edge(ResolvedThrowCondition condition, TrieNode child) {}

    /**
     * Key that is safe even when SymExpr.toString() is buggy for some expression kinds.
     */
    private record EdgeKey(boolean truth, String exprKey) {
        static EdgeKey from(ResolvedThrowCondition c) {
            boolean t = c != null && c.isTruthValue();
            String k = safeExprKey(c == null ? null : c.getNode());
            return new EdgeKey(t, k);
        }

        private static String safeExprKey(SymExpr expr) {
            if (expr == null) return "<null>";
            try {
                return String.valueOf(expr);
            } catch (Throwable t) {
                return "<unprintable:" + expr.getClass().getSimpleName() + ":" + t.getClass().getSimpleName() + ">";
            }
        }
    }
}

