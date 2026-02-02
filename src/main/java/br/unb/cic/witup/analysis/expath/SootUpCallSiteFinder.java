package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.expath.records.CallSite;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.InvokableStmt;
import sootup.core.jimple.common.stmt.Stmt;

public final class SootUpCallSiteFinder implements CallSiteFinder {

    private final Set<String> knownMethodSignatures;

    public SootUpCallSiteFinder(final Set<String> knownMethodSignatures) {
        this.knownMethodSignatures = knownMethodSignatures;
    }

    @Override
    public List<CallSite> findCallSites(final WITUpGraph callerCfg) {
        List<CallSite> out = new ArrayList<>();

        for (WITUpNode n : callerCfg.vertexSet()) {
            if (!(n.getNode() instanceof StmtGraphNode stmtNode)) continue;

            Stmt stmt = stmtNode.getStmt();
            if (!stmt.isInvokableStmt()) continue;

            InvokableStmt inv = stmt.asInvokableStmt();
            var invokeOpt = inv.getInvokeExpr();
            if (invokeOpt.isEmpty()) continue;

            Object obj = invokeOpt.get();
            if (!(obj instanceof AbstractInvokeExpr invoke)) continue;

            String calleeSig = invoke.getMethodSignature().toString();
            if (!knownMethodSignatures.contains(calleeSig)) continue;

            List<String> args = new ArrayList<>();
            for (Value v : invoke.getArgs()) args.add(v.toString());

            out.add(new CallSite(calleeSig, args, n));
        }

        return out;
    }
}

