package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
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
    PropertyGraph cfg = new CfgCreator().createGraph(method);
    PropertyGraph cdg = new CdgCreator().createGraph(method);
    PropertyGraph ddg = new DdgCreator().createGraph(method);

    PropertyGraph cpg = PropertyGraphsMerger.mergeGraphs(cfg, cdg);
    cpg = PropertyGraphsMerger.mergeGraphs(cpg, ddg);

    return WITUpGraph.fromPropertyGraph(cpg, method.getSignature().toString());
  }
}
