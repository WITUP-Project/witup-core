package br.unb.cic.witup.analysis.graph;


//import guru.nidi.graphviz.engine.Format;
//import guru.nidi.graphviz.engine.Graphviz;
//import java.io.File;
import sootup.codepropertygraph.cdg.CdgCreator;
import sootup.codepropertygraph.cfg.CfgCreator;
import sootup.codepropertygraph.ddg.DdgCreator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.util.PropertyGraphsMerger;
import sootup.java.core.JavaSootMethod;

public final class CPGBuilder {
  private CPGBuilder() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static WITUpGraph buildForMethod(final JavaSootMethod method) {
    PropertyGraph cpg = buildCpg(method);
//            String dot = cpg.toDotGraph();
//            try {
//              File graphsDir = new File("graphs");
//
//              if (!graphsDir.exists() && !graphsDir.mkdirs()) {
//                throw new RuntimeException("Could not create graphs directory");
//              }
//
//              File output = new File(graphsDir, method.getSignature() + ".svg");
//
//              Graphviz.fromString(dot)
//                      .render(Format.SVG)
//                      .toFile(output);
//            } catch (Exception e) {
//              e.printStackTrace();
//            }

    return WITUpGraph.fromPropertyGraph(cpg, method);
  }

  public static PropertyGraph buildCpg(final JavaSootMethod method) {
    PropertyGraph cfg = new CfgCreator().createGraph(method);
    PropertyGraph cdg = new CdgCreator().createGraph(method);
    PropertyGraph ddg = new DdgCreator().createGraph(method);
    PropertyGraph cpg = PropertyGraphsMerger.mergeGraphs(cfg, cdg);
    return PropertyGraphsMerger.mergeGraphs(cpg, ddg);
  }
}
