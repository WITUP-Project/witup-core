package br.unb.cic.witup.analysis;

import sootup.codepropertygraph.cdg.CdgCreator;
import sootup.codepropertygraph.cfg.CfgCreator;
import sootup.codepropertygraph.ddg.DdgCreator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.util.PropertyGraphsMerger;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

/**
 * Entry point of the analysis pipeline. Analyses a class given its location and name. For each
 * method to be analysed, build the individual graphs and the resulting Code Property Graph (CPG)
 */
public final class SootUpClassAnalyser {
  private final String location;
  private final String className;

  public SootUpClassAnalyser(final String location, final String className) {
    this.location = location;
    this.className = className;
  }

  public JavaSootClass getSootClass() {
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(location);
    JavaView view = new JavaView(inputLocation);
    JavaClassType classType = view.getIdentifierFactory().getClassType(className);

    JavaSootClass sootClass =
        view.getClass(classType)
            .orElseThrow(() -> new RuntimeException("Soot class not found: " + classType));

    return sootClass;
  }

  public PropertyGraph buildCPG(final JavaSootMethod m) {
    CfgCreator cfgCreator = new CfgCreator();
    CdgCreator cdgCreator = new CdgCreator();
    DdgCreator ddgCreator = new DdgCreator();

    PropertyGraph cfg = cfgCreator.createGraph(m);
    PropertyGraph cdg = cdgCreator.createGraph(m);
    PropertyGraph ddg = ddgCreator.createGraph(m);

    PropertyGraph cpg = cfg;
    cpg = PropertyGraphsMerger.mergeGraphs(cpg, cdg);
    cpg = PropertyGraphsMerger.mergeGraphs(cpg, ddg);
    return cpg;
  }
}
