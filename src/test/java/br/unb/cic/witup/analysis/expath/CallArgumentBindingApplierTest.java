package br.unb.cic.witup.analysis.expath;

import static org.junit.jupiter.api.Assertions.*;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.Stmt;

public class CallArgumentBindingApplierTest {

    private Path testClassesDir;
    private SootUpAnalyser sootUpAnalyser;

    @BeforeEach
    void setUp() {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        testClassesDir = projectRoot.resolve("target/test-classes");
        sootUpAnalyser = new SootUpAnalyser();
    }

    @Test
    void shouldApplyBindingUsingSootUpValues() {
        HashMap<String, SootUpPropertyGraphs> graphs =
                sootUpAnalyser.analyseThrowingMethods(
                        testClassesDir.toString(), "br.unb.cic.witup.samples.SampleMean");

        String rootSig = "<br.unb.cic.witup.samples.SampleMean: double processSample(double,int)>";
        GlobalExpathComposer composer = new GlobalExpathComposer();

        List<List<ResolvedThrowCondition>> globals = composer.composeGlobals(graphs, rootSig, 2);
        assertNotNull(globals);
        assertFalse(globals.isEmpty());

        // Extract invoke args from processSample -> meanChecked(sum, n)
        SootUpPropertyGraphs rootGraphs = graphs.get(rootSig);
        assertNotNull(rootGraphs);

        WITUpGraph cfg = WITUpGraph.fromPropertyGraph(rootGraphs.getCFG());
        AbstractInvokeExpr invoke = findInvokeByMethodName(cfg, "meanChecked");
        assertNotNull(invoke);

        // Intentionally bind n -> arg0 (sum) so we can observe a visible change in expressions.
        // (For test visibility only; semantically you'd map n -> arg1.)
        CallArgumentBinding binding =
                new CallArgumentBinding(Map.of("n", invoke.getArgs().get(0)));

        CallArgumentBindingApplier applier = new CallArgumentBindingApplier();
        List<List<ResolvedThrowCondition>> bound = applier.apply(globals, List.of(binding));

        assertNotNull(bound);
        assertEquals(globals.size(), bound.size());

        // After binding n -> sum, at least one condition previously containing "n"
        // should contain "sum" in the rendered expression.
        boolean hasSum = bound.stream()
                .flatMap(List::stream)
                .map(c -> c.getNode().toString())
                .anyMatch(s -> s.contains("sum"));

        assertTrue(hasSum, "Expected at least one bound condition containing 'sum'.");
    }

    private static AbstractInvokeExpr findInvokeByMethodName(WITUpGraph cfg, String methodName) {
        for (WITUpNode n : cfg.vertexSet()) {
            if (!(n.getNode() instanceof StmtGraphNode stmtNode)) continue;
            Stmt stmt = stmtNode.getStmt();

            Optional<AbstractInvokeExpr> maybe = invokeExprOf(stmt);
            if (maybe.isPresent() && maybe.get().getMethodSignature().toString().contains(methodName)) {
                return maybe.get();
            }
        }
        throw new AssertionError("Could not find invoke expr for method: " + methodName);
    }

    private static Optional<AbstractInvokeExpr> invokeExprOf(Stmt stmt) {
        if (stmt instanceof JInvokeStmt invStmt) {
            return toOptionalInvokeExpr(invStmt.getInvokeExpr());
        }
        if (stmt instanceof JAssignStmt assignStmt) {
            return toOptionalInvokeExpr(assignStmt.getInvokeExpr());
        }
        return Optional.empty();
    }

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
