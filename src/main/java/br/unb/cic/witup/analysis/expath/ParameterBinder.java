package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.SymConst;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.SymVar;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.stmt.Stmt;

/**
 * Binds callee-local parameter variables to the caller's actual arguments at a callsite.
 *
 * <p>This is required for interprocedural inlining to produce meaningful global expaths: local
 * conditions inside the callee typically refer to locals that receive @parameterN. We map those
 * locals to the callsite argument expressions.
 */
public final class ParameterBinder {

    private static final Pattern PARAM_REF = Pattern.compile("@parameter(\\d+)");

    private final ExpathCache cache;

    public ParameterBinder() {
        this(null);
    }

    public ParameterBinder(final ExpathCache cache) {
        this.cache = cache;
    }

    public Map<String, SymExpr> bindCalleeParameterLocalsToArgs(
            final String calleeSignature,
            final SootUpPropertyGraphs calleeGraphs,
            final List<String> callArgs) {

        Objects.requireNonNull(calleeSignature, "calleeSignature");
        Objects.requireNonNull(calleeGraphs, "calleeGraphs");
        Objects.requireNonNull(callArgs, "callArgs");

        Map<Integer, String> idxToLocal = getOrComputeParamIndexToLocal(calleeSignature, calleeGraphs);

        Map<String, SymExpr> subst = new HashMap<>();
        for (int i = 0; i < callArgs.size(); i++) {
            String local = idxToLocal.get(i);
            if (local == null) continue;
            subst.put(local, argToSymExpr(callArgs.get(i)));
        }

        return subst;
    }

    private Map<Integer, String> getOrComputeParamIndexToLocal(
            final String calleeSignature,
            final SootUpPropertyGraphs calleeGraphs) {

        if (cache != null) {
            Optional<Map<Integer, String>> cached = cache.getParamIndexToLocal(calleeSignature);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        WITUpGraph calleeCfg = WITUpGraph.fromPropertyGraph(calleeGraphs.getCFG());

        Map<Integer, String> idxToLocal = new HashMap<>();
        for (WITUpNode n : calleeCfg.vertexSet()) {
            if (!(n.getNode() instanceof StmtGraphNode stmtNode)) continue;

            Stmt stmt = stmtNode.getStmt();
            String s = stmt.toString();

            // We look for the Jimple-ish identity assignment: <local> := @parameterN
            if (!s.contains("@parameter") || !s.contains(":=")) continue;

            Matcher m = PARAM_REF.matcher(s);
            if (!m.find()) continue;

            int idx;
            try {
                idx = Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                continue;
            }

            String lhs = s.split(":=")[0].trim();
            if (!lhs.isEmpty()) {
                idxToLocal.put(idx, lhs);
            }
        }

        Map<Integer, String> immutable = Collections.unmodifiableMap(idxToLocal);

        if (cache != null) {
            cache.putParamIndexToLocal(calleeSignature, immutable);
        }

        return immutable;
    }

    private static SymExpr argToSymExpr(final String raw) {
        String a = raw.trim();
        if (a.matches("-?\\d+")) return new SymConst(Integer.parseInt(a));
        if (a.matches("-?\\d+\\.\\d+")) return new SymConst(Double.parseDouble(a));
        return new SymVar(a);
    }
}
