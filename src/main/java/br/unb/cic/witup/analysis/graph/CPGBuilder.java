package br.unb.cic.witup.analysis.graph;

import java.io.File;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
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
    String dot = cpg.toDotGraph();
    try {
      Graphviz.fromString(dot)
              .render(Format.SVG)
              .toFile(new File(System.getProperty("user.dir"), method.getSignature().toString()));
    } catch (Exception e) {
      e.printStackTrace();
    }

    return WITUpGraph.fromPropertyGraph(cpg, method.getSignature().toString());
  }

  public static PropertyGraph buildCpg(final JavaSootMethod method) {
    PropertyGraph cfg = new CfgCreator().createGraph(method);
    PropertyGraph cdg = new CdgCreator().createGraph(method);
    PropertyGraph ddg = new DdgCreator().createGraph(method);
    PropertyGraph cpg = PropertyGraphsMerger.mergeGraphs(cfg, cdg);
    return PropertyGraphsMerger.mergeGraphs(cpg, ddg);
  }
}
