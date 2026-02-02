package br.unb.cic.witup.helper;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymBinOp;
import br.unb.cic.witup.analysis.SymConst;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.SymField;
import br.unb.cic.witup.analysis.SymVar;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.IfStatementNode;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;

/**
 * Pretty printer de expaths no estilo do paper:
 *  - local expaths como sequência ifK -> ... -> throwM com guards nas "arestas"
 *  - resolved paths como conjunções (uma linha por path)
 */
public final class ExpathPrettyPrinter {

    private ExpathPrettyPrinter() {}

    // ============================================================
    // PUBLIC API
    // ============================================================

    /** Paper-like: imprime LOCAL expaths "crus" do CFG (guards nas boolean edges). */
    public static void printLocalExpathsRawCfgGuards(
            String methodSignature, SootUpPropertyGraphs pg) {

        WITUpGraph cfg = WITUpGraph.fromPropertyGraph(pg.getCFG());
        Map<WITUpNode, String> ids = assignPaperLikeIds(cfg);

        System.out.println("\n==================================================");
        System.out.println("LOCAL EXCEPTION PATHS (RAW CFG guards) for: " + methodSignature);
        System.out.println("==================================================\n");

        List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(cfg).stream()
                .sorted(Comparator.comparing(n -> ids.getOrDefault(n, "throw9999")))
                .collect(Collectors.toList());

        int throwIdx = 0;
        for (WITUpNode tn : throwNodes) {
            throwIdx++;
            if (!(tn instanceof ThrowStatementNode)) continue;

            String throwId = ids.getOrDefault(tn, "throw" + throwIdx);
            String throwStmt = prettyStmt(tn);

            List<List<ThrowCondition>> condPaths =
                    ConditionPathFinder.findConditionPathsToNode(cfg, tn);

            System.out.println("-- THROW #" + throwIdx + " (" + throwId + ")");
            System.out.println("   throw-stmt: " + throwStmt);
            System.out.println("   paths=" + condPaths.size());

            for (int i = 0; i < condPaths.size(); i++) {
                List<ThrowCondition> path = condPaths.get(i);

                // Reconstroi um "paper path" como sequência de nós IF + THROW
                PaperLocalPath paperPath = rebuildPaperLocalPath(cfg, tn, path, ids);

                System.out.println("   p" + (i + 1) + " : " + paperPath.headId);
                for (PaperStep step : paperPath.steps) {
                    // Ex.: "a!=null -----> if3"
                    System.out.println("     " + step.guardLabel + "  -----> " + step.nextNodeId);
                }
                System.out.println();
            }
        }
    }

    /** Imprime local/global resolved no formato "pK : c1, c2, ...". */
    public static void printResolvedPaths(
            String title, String methodSignature, List<List<ResolvedThrowCondition>> paths) {

        System.out.println("\n==================================================");
        System.out.println(title + " for: " + methodSignature);
        System.out.println("count=" + (paths == null ? 0 : paths.size()));
        System.out.println("==================================================");

        if (paths == null || paths.isEmpty()) {
            System.out.println("  (no paths)");
            return;
        }

        for (int i = 0; i < paths.size(); i++) {
            List<ResolvedThrowCondition> p = paths.get(i);

            // estilo paper: p1 : c1, c2, c3
            String oneLiner = p.stream()
                    .filter(Objects::nonNull)
                    .map(ExpathPrettyPrinter::prettyResolvedGuard)
                    .collect(Collectors.joining(", "));

            System.out.println("  p" + (i + 1) + " : " + oneLiner);
        }
    }

    // ============================================================
    // PAPER-LIKE LOCAL PATH RECONSTRUCTION
    // ============================================================

    private static PaperLocalPath rebuildPaperLocalPath(
            WITUpGraph cfg,
            WITUpNode throwNode,
            List<ThrowCondition> conds,
            Map<WITUpNode, String> ids) {

        WITUpNode entry = WITUpGraph.findEntryNode(cfg);

        AllDirectedPaths<WITUpNode, WITUpEdge> all = new AllDirectedPaths<>(cfg);
        List<GraphPath<WITUpNode, WITUpEdge>> allPaths = all.getAllPaths(entry, throwNode, true, null);

        List<WITUpNode> wantedIfNodes = conds.stream()
                .map(ThrowCondition::getNode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        GraphPath<WITUpNode, WITUpEdge> chosen =
                allPaths.stream()
                        .filter(p -> containsIfSequence(p.getVertexList(), wantedIfNodes))
                        .findFirst()
                        .orElse(null);

        if (chosen == null) {
            String head = wantedIfNodes.isEmpty()
                    ? ids.getOrDefault(throwNode, "throw?")
                    : ids.getOrDefault(wantedIfNodes.get(0), "if?");
            List<PaperStep> steps = new ArrayList<>();
            for (ThrowCondition tc : conds) {
                String guard = prettyIfGuard(tc);
                steps.add(new PaperStep(guard, "<next?>"));
            }
            steps.add(new PaperStep("<reach>", ids.getOrDefault(throwNode, "throw?")));
            return new PaperLocalPath(head, steps);
        }

        List<WITUpNode> vertices = chosen.getVertexList();
        Map<WITUpNode, Boolean> truthByIf = new IdentityHashMap<>();
        for (ThrowCondition tc : conds) {
            truthByIf.put(tc.getNode(), tc.getTruthValue());
        }

        WITUpNode headNode =
                vertices.stream().filter(v -> v instanceof IfStatementNode).findFirst().orElse(vertices.get(0));
        String headId = ids.getOrDefault(headNode, prettyIdFallback(headNode));

        List<PaperStep> steps = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            WITUpNode v = vertices.get(i);
            if (!(v instanceof IfStatementNode)) continue;

            boolean truth = truthByIf.getOrDefault(v, true);
            String guardLabel = prettyIfGuard((IfStatementNode) v, truth);

            String nextId = null;
            for (int j = i + 1; j < vertices.size(); j++) {
                WITUpNode nxt = vertices.get(j);
                if (nxt instanceof IfStatementNode || nxt instanceof ThrowStatementNode) {
                    nextId = ids.getOrDefault(nxt, prettyIdFallback(nxt));
                    break;
                }
            }
            if (nextId == null) {
                nextId = ids.getOrDefault(throwNode, "throw?");
            }

            steps.add(new PaperStep(guardLabel, nextId));
        }

        if (steps.isEmpty()) {
            steps.add(new PaperStep("<reach>", ids.getOrDefault(throwNode, "throw?")));
        }

        return new PaperLocalPath(headId, steps);
    }

    private static boolean containsIfSequence(List<WITUpNode> vertices, List<WITUpNode> wanted) {
        if (wanted.isEmpty()) return true;
        int pos = 0;
        for (WITUpNode v : vertices) {
            if (v.equals(wanted.get(pos))) {
                pos++;
                if (pos == wanted.size()) return true;
            }
        }
        return false;
    }

    // ============================================================
    // ID ASSIGNMENT (if2, throw5, ...)
    // ============================================================

    private static Map<WITUpNode, String> assignPaperLikeIds(WITUpGraph g) {
        List<WITUpNode> nodes = new ArrayList<>(g.vertexSet());
        nodes.sort(Comparator.comparing(ExpathPrettyPrinter::prettyStmt));

        int ifCount = 0;
        int throwCount = 0;
        int nodeCount = 0;

        Map<WITUpNode, String> ids = new HashMap<>();
        for (WITUpNode n : nodes) {
            if (n instanceof IfStatementNode) {
                ifCount++;
                ids.put(n, "if" + ifCount);
            } else if (n instanceof ThrowStatementNode) {
                throwCount++;
                ids.put(n, "throw" + throwCount);
            } else {
                nodeCount++;
                ids.put(n, "n" + nodeCount);
            }
        }
        return ids;
    }

    private static String prettyIdFallback(WITUpNode n) {
        if (n instanceof IfStatementNode) return "if?";
        if (n instanceof ThrowStatementNode) return "throw?";
        return "n?";
    }

    // ============================================================
    // IF GUARD PRINTING (RAW)
    // ============================================================

    private static String prettyIfGuard(ThrowCondition tc) {
        if (tc == null) return "<null>";
        WITUpNode n = tc.getNode();
        if (!(n instanceof IfStatementNode)) return "<non-if>";
        return prettyIfGuard((IfStatementNode) n, tc.getTruthValue());
    }

    private static String prettyIfGuard(IfStatementNode ifNode, boolean takeTrue) {
        String cond = safeValueString(ifNode.getCondition());
        if (takeTrue) {
            return cond;
        }
        return "!(" + cond + ")";
    }

    // ============================================================
    // RESOLVED GUARD PRINTING (1-LINER PAPER STYLE)
    // ============================================================

    private static String prettyResolvedGuard(ResolvedThrowCondition c) {
        if (c == null) return "<null>";
        String expr = safeExprString(c.getNode());

        if (c.isTruthValue()) {
            return "(" + expr + ")";
        }
        return "!(" + "(" + expr + ")" + ")";
    }

    // ============================================================
    // SAFE STRINGIFIERS
    // ============================================================

    private static String safeExprString(SymExpr expr) {
        if (expr == null) return "<null-expr>";
        try {
            if (expr instanceof SymBinOp b) {
                return prettySymBinOp(b);
            }
            if (expr instanceof SymVar v) {
                return v.getName();
            }
            if (expr instanceof SymField f) {
                return f.getBase() + "." + f.getFieldName();
            }
            if (expr instanceof SymConst k) {
                return String.valueOf(k.getValue());
            }
            return String.valueOf(expr);
        } catch (Throwable t) {
            return "<unprintable " + expr.getClass().getSimpleName() + ": " + t.getClass().getSimpleName() + ">";
        }
    }

    private static String prettySymBinOp(SymBinOp b) {
        String l = safeExprString(b.getLeft());
        String r = safeExprString(b.getRight());
        return l + " " + b.getOp() + " " + r;
    }

    private static String safeValueString(Object v) {
        if (v == null) return "<null>";
        try {
            return String.valueOf(v);
        } catch (Throwable t) {
            return "<unprintable:" + v.getClass().getSimpleName() + ":" + t.getClass().getSimpleName() + ">";
        }
    }

    private static String prettyStmt(WITUpNode n) {
        if (n == null) return "<null-node>";
        Object pg = n.getNode();
        if (pg == null) return "<null-pg>";
        try {
            return String.valueOf(pg);
        } catch (Throwable t) {
            return "<unprintable:" + pg.getClass().getSimpleName() + ":" + t.getClass().getSimpleName() + ">";
        }
    }

    // ============================================================
    // INTERNAL DATA
    // ============================================================

    private record PaperLocalPath(String headId, List<PaperStep> steps) {}
    private record PaperStep(String guardLabel, String nextNodeId) {}

    // ============================================================
    // Minimal bounded condition finder for the pretty printer
    // (keeps tests independent of production bounded finder if needed)
    // ============================================================

    public static final class ConditionPathFinder {
        private ConditionPathFinder() {}

        public static List<List<ThrowCondition>> findConditionPathsToNode(
                final WITUpGraph cfg, final WITUpNode targetNode) {

            WITUpNode entry = WITUpGraph.findEntryNode(cfg);

            AllDirectedPaths<WITUpNode, WITUpEdge> allPaths = new AllDirectedPaths<>(cfg);
            List<GraphPath<WITUpNode, WITUpEdge>> paths = allPaths.getAllPaths(entry, targetNode, true, null);

            List<List<ThrowCondition>> conditionsByPath = new ArrayList<>();
            for (GraphPath<WITUpNode, WITUpEdge> path : paths) {
                List<ThrowCondition> pathConditions = new ArrayList<>();
                for (WITUpEdge edge : path.getEdgeList()) {
                    if (edge instanceof BooleanCFGEdge) {
                        BooleanCFGEdge be = (BooleanCFGEdge) edge;
                        pathConditions.add(new ThrowCondition(be.getSource(), be.getCondition()));
                    }
                }
                conditionsByPath.add(pathConditions);
            }

            return conditionsByPath;
        }
    }
}
