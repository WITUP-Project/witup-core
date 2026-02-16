package br.unb.cic.witup.graph;

import br.unb.cic.witup.sootup.SootUpAnalyser;
import java.util.HashMap;
import java.util.Set;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class WITUpAnalyser {
  private final SootUpAnalyser sootUpAnalyser;

  public WITUpAnalyser(String location, String className) {
    this.sootUpAnalyser = new SootUpAnalyser(location, className);
  }

  public HashMap<String, WITUpGraph> buildWitUpGraphs() {
    HashMap<String, WITUpGraph> witUpGraphs = new HashMap<>();

    JavaSootClass sootClass = sootUpAnalyser.getSootClass();
    Set<JavaSootMethod> methods = sootClass.getMethods();
    methods.forEach(
        m -> {
          Body body = m.getBody();
          StmtGraph<?> graph = body.getStmtGraph();

          for (Stmt s : graph) {
            if (s instanceof JThrowStmt) {
              witUpGraphs.put(
                  m.getSignature().toString(),
                  WITUpGraph.fromPropertyGraph(sootUpAnalyser.buildCPG(m)));
              break;
            }
          }
        });
    return witUpGraphs;
  }
}
