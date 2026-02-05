package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;

/**
 * Minimal global expath composer with N-depth inlining.
 * global = prefix(entry->callsite) + calleeGlobal(depth-1)
 *
 */
public final class GlobalExpathComposer {

    /**
     * Builds global expaths starting at {@code rootSignature}, inlining callees up to {@code maxDepth}.
     *
     * <p>{@code maxDepth = 0} means: only local expaths (throws) of the root method.</p>
     */
    public List<List<ResolvedThrowCondition>> composeGlobals(
            Map<String, SootUpPropertyGraphs> graphsBySignature,
            String rootSignature,
            int maxDepth
    ) {
        Objects.requireNonNull(graphsBySignature, "graphsBySignature");
        Objects.requireNonNull(rootSignature, "rootSignature");
        if (maxDepth < 0) throw new IllegalArgumentException("maxDepth must be >= 0");

        return composeRec(graphsBySignature, rootSignature, maxDepth, new HashSet<>());
    }

    private List<List<ResolvedThrowCondition>> composeRec(
            Map<String, SootUpPropertyGraphs> graphsBySignature,
            String methodSignature,
            int depthLeft,
            Set<String> inStack
    ) {
        SootUpPropertyGraphs graphs = graphsBySignature.get(methodSignature);
        if (graphs == null) return List.of();

        // avoid infinite recursion on cycles
        if (!inStack.add(methodSignature)) return List.of();

        WITUpGraph cfg = WITUpGraph.fromPropertyGraph(graphs.getCFG());
        WITUpGraph ddg = WITUpGraph.fromPropertyGraph(graphs.getDDG());
        Resolver resolver = new Resolver(ddg);

        List<List<ResolvedThrowCondition>> out = new ArrayList<>();

        // 1) Always include local throws of this method.
        for (WITUpNode throwNode : WITUpGraph.findThrowNodes(cfg)) {
            var localPaths = WITUpGraph.findConditionPaths(cfg, throwNode);
            out.addAll(resolver.resolveConditionPaths(localPaths, ddg));
        }

        // 2) Stop if no more depth.
        if (depthLeft == 0) {
            inStack.remove(methodSignature);
            return out;
        }

        // 3) Inline callees: prefix(entry->callsite) + calleeGlobals(depth-1)
        for (CallSite site : findCallSites(cfg)) {
            String calleeSig = site.calleeSignature();
            if (!graphsBySignature.containsKey(calleeSig)) continue;

            var prefixLocalPaths = WITUpGraph.findConditionPaths(cfg, site.callNode());
            var prefixResolved = resolver.resolveConditionPaths(prefixLocalPaths, ddg);

            var calleeGlobals = composeRec(graphsBySignature, calleeSig, depthLeft - 1, inStack);

            for (var p : prefixResolved) {
                for (var g : calleeGlobals) {
                    out.add(concat(p, g));
                }
            }
        }

        inStack.remove(methodSignature);
        return out;
    }

    private static List<ResolvedThrowCondition> concat(
            List<ResolvedThrowCondition> a,
            List<ResolvedThrowCondition> b
    ) {
        List<ResolvedThrowCondition> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    /**
     * Callsite:
     * - scans statement nodes in CFG
     * - extracts invoke expr from known statement types
     * - maps invoke expr -> method signature string
     */
    private static List<CallSite> findCallSites(WITUpGraph cfg) {
        List<CallSite> out = new ArrayList<>();
        for (WITUpNode n : cfg.vertexSet()) {
            if (!(n.getNode() instanceof StmtGraphNode stmtNode)) continue;
            Stmt stmt = stmtNode.getStmt();

            calleeSignatureOf(stmt).ifPresent(sig -> out.add(new CallSite(sig, n)));
        }
        return out;
    }

    private static Optional<String> calleeSignatureOf(Stmt stmt) {
        return invokeExprOf(stmt)
                .map(invokeExpr -> invokeExpr.getMethodSignature().toString());
    }

    /**
     * Extracts an invoke expression from a generic statement, if present.
     *
     * Pseudocode examples of callsites this method can detect:
     * - invoke-only statement:
     *     foo(a, b);
     * - assignment with invocation:
     *     x = foo(a, b);
     */
    private static Optional<AbstractInvokeExpr> invokeExprOf(Stmt stmt) {
        return invokeExprFromInvokeStmt(stmt)
                .or(() -> invokeExprFromAssignStmt(stmt));
    }

    /**
     * Handles callsites represented as invoke-only statements (JInvokeStmt).
     *
     * Pseudocode:
     *   foo(a, b);
     *
     * In Jimple-like IR this is typically a standalone invoke statement.
     */
    private static Optional<AbstractInvokeExpr> invokeExprFromInvokeStmt(Stmt stmt) {
        if (stmt instanceof JInvokeStmt invStmt) {
            return toOptionalInvokeExpr(invStmt.getInvokeExpr());
        }
        return Optional.empty();
    }

    /**
     * Handles callsites represented as assignment statements (JAssignStmt)
     * where the right-hand side is an invocation.
     *
     * Pseudocode:
     *   x = foo(a, b);
     *
     * If the assignment is not an invocation assignment, returns empty.
     */
    private static Optional<AbstractInvokeExpr> invokeExprFromAssignStmt(Stmt stmt) {
        if (stmt instanceof JAssignStmt assignStmt) {
            return toOptionalInvokeExpr(assignStmt.getInvokeExpr());
        }
        return Optional.empty();
    }

    /**
     * Normalizes either:
     * - AbstractInvokeExpr
     * - Optional<AbstractInvokeExpr>
     */
    @SuppressWarnings("unchecked")
    private static Optional<AbstractInvokeExpr> toOptionalInvokeExpr(Object maybeInvokeExpr) {
        if (maybeInvokeExpr == null) return Optional.empty();

        if (maybeInvokeExpr instanceof Optional<?> opt) {
            return (Optional<AbstractInvokeExpr>) opt;
        }
        if (maybeInvokeExpr instanceof AbstractInvokeExpr invokeExpr) {
            return Optional.of(invokeExpr);
        }
        return Optional.empty();
    }

}
