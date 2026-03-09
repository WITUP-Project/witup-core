package br.unb.cic.witup.analysis.graph;

import java.util.HashMap;
import java.util.Set;

import br.unb.cic.witup.analysis.ClassAnalyser;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public final class WITUpAnalyser {
  private final ClassAnalyser classAnalyser;

  public WITUpAnalyser(final String location, final String className) {
    this.classAnalyser = new ClassAnalyser(location, className);
  }

  public HashMap<String, WITUpGraph> buildWitUpGraphs() {
    HashMap<String, WITUpGraph> witUpGraphs = new HashMap<>();

    JavaSootClass sootClass = classAnalyser.getSootClass();
    Set<JavaSootMethod> methods = sootClass.getMethods();
    methods.forEach(
        m -> {
          Body body = m.getBody();
          StmtGraph<?> graph = body.getStmtGraph();

          //          PropertyGraph g = sootUpAnalyser.buildCPG(m);
          //          String dot = g.toDotGraph();
          //          try {
          //            Graphviz.fromString(dot).render(Format.SVG).toFile(new File(m.getSignature()
          // + ".svg"));
          //          } catch (IOException e) {
          //            throw new RuntimeException(e);
          //          }

          for (Stmt s : graph) {
            if (s instanceof JThrowStmt) {
              witUpGraphs.put(
                  m.getSignature().toString(),
                  WITUpGraph.fromPropertyGraph(
                      classAnalyser.buildCPG(m), m.getSignature().toString()));
              break;
            }
          }
        });
    return witUpGraphs;
  }
}
