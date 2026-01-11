package br.unb.cic.witup.sootup;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.util.HashMap;
import java.util.Set;

/**
 * Entry point of the analysis pipeline. Analyses a class given its location
 * and name. For each method to be analysed, build the individual graphs and
 * the resulting Code Property Graph (CPG)
 */
public final class SootUpAnalyser {
    private final SootUpGraphBuilder sootUpGraphBuilder;

    public SootUpAnalyser() {
        this.sootUpGraphBuilder = new SootUpGraphBuilder();
    }

    public HashMap<String, SootUpPropertyGraphs> analyseThrowingMethods(
            final String location,
            final String className
    ) {
        AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(location);
        JavaView view = new JavaView(inputLocation);
        JavaClassType classType = view.getIdentifierFactory().getClassType(className);

        JavaSootClass sootClass = view.getClass(classType)
                .orElseThrow(() -> new RuntimeException("Soot class not found: " + classType));

        return buildSootUpPropertyGraphs(sootClass);
    }

    private HashMap<String, SootUpPropertyGraphs> buildSootUpPropertyGraphs(JavaSootClass sootClass) {
        HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs = new HashMap<>();

        Set<JavaSootMethod> methods = sootClass.getMethods();
        methods.forEach(m -> {
            Body body = m.getBody();
            StmtGraph<?> graph = body.getStmtGraph();

            for (Stmt s : graph) {
                if (s instanceof JThrowStmt) {
                    sootUpPropertyGraphs.put(
                            m.getSignature().toString(),
                            sootUpGraphBuilder.buildPropertyGraphs(m));
                    break;
                }
            }
        });
        return sootUpPropertyGraphs;
    }
}
