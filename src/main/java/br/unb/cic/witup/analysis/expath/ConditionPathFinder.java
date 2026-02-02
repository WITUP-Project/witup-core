package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility: find conditional paths from CFG entry to an arbitrary target node.
 *
 * <p>Important: paths with no boolean edges are kept as an empty condition list (unconditional
 * reachability).
 *
 * <p>Scalability: path enumeration is bounded by {@link ExpathOptions}.
 */
public final class ConditionPathFinder {

    private ConditionPathFinder() {}

    public static List<List<ThrowCondition>> findConditionPathsToNode(
            final WITUpGraph cfg, final WITUpNode targetNode) {
        return findConditionPathsToNode(cfg, targetNode, ExpathOptions.DEFAULT);
    }

    public static List<List<ThrowCondition>> findConditionPathsToNode(
            final WITUpGraph cfg, final WITUpNode targetNode, final ExpathOptions options) {

        if (cfg == null) {
            throw new IllegalArgumentException("cfg must not be null");
        }
        if (targetNode == null) {
            throw new IllegalArgumentException("targetNode must not be null");
        }

        WITUpNode entry = WITUpGraph.findEntryNode(cfg);

        List<List<ThrowCondition>> out = new ArrayList<>();
        List<ThrowCondition> guards = new ArrayList<>();
        Set<WITUpNode> inPath = new HashSet<>();

        dfs(cfg, entry, targetNode, 0, options, guards, inPath, out);
        return out;
    }

    private static void dfs(
            final WITUpGraph cfg,
            final WITUpNode current,
            final WITUpNode target,
            final int depth,
            final ExpathOptions options,
            final List<ThrowCondition> guards,
            final Set<WITUpNode> inPath,
            final List<List<ThrowCondition>> out) {

        if (out.size() >= options.maxPaths()) {
            return;
        }
        if (depth > options.maxDepth()) {
            return;
        }

        if (current.equals(target)) {
            out.add(new ArrayList<>(guards));
            return;
        }

        // avoid cycles (simple paths), consistent with AllDirectedPaths(..., simplePaths=true)
        if (!inPath.add(current)) {
            return;
        }

        for (WITUpEdge e : cfg.outgoingEdgesOf(current)) {
            if (out.size() >= options.maxPaths()) {
                break;
            }

            WITUpNode next = cfg.getEdgeTarget(e);

            int before = guards.size();
            if (e instanceof BooleanCFGEdge be) {
                guards.add(new ThrowCondition(be.getSource(), be.getCondition()));
            }

            dfs(cfg, next, target, depth + 1, options, guards, inPath, out);

            // backtrack guards list
            while (guards.size() > before) {
                guards.remove(guards.size() - 1);
            }
        }

        inPath.remove(current);
    }
}
