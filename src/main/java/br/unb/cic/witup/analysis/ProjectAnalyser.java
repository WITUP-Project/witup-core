package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.codepropertygraph.cdg.CdgCreator;
import sootup.codepropertygraph.cfg.CfgCreator;
import sootup.codepropertygraph.ddg.DdgCreator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.util.PropertyGraphsMerger;
import sootup.core.graph.StmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.Body;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public final class ProjectAnalyser {
  // assumes jar ia witup-core/project-jars
  private final Path jarPath;
  private static final Logger log = LoggerFactory.getLogger("ProjectAnalyser");

  public ProjectAnalyser(final Path jarPath) {
    this.jarPath = jarPath;
  }

  public Map<String, WITUpGraph> analyseProject() {
    AnalysisInputLocation inputLocation =
        new JavaClassPathAnalysisInputLocation(jarPath.toAbsolutePath().toString());
    JavaView view = new JavaView(inputLocation);
    List<JavaSootClass> classes = view.getClasses().toList();
    log.info("Found {} classes", classes.size());
    log.info(classes.toString());

    return analyseClasses(classes);
  }

  private Map<String, WITUpGraph> analyseClasses(final List<JavaSootClass> classes) {
    return classes.stream()
        .map(this::analyseClass)
        .flatMap(m -> m.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, WITUpGraph> analyseClass(final JavaSootClass sootClass) {
    return sootClass.getMethods().stream()
        .filter(JavaSootMethod::hasBody)
        .filter(this::methodHasThrow) // filter only throwing methods here
        .collect(
            Collectors.toMap(
                method -> method.getSignature().toString(), this::analyseThrowingMethod));
  }

  private boolean methodHasThrow(final JavaSootMethod method) {
    return method.getBody().getStmtGraph().getNodes().stream()
        .anyMatch(s -> s instanceof JThrowStmt);
  }

  public WITUpGraph analyseThrowingMethod(final JavaSootMethod sootMethod) {
    Body body = sootMethod.getBody();
    StmtGraph<?> graph = body.getStmtGraph();
    for (Stmt s : graph) {
      if (s instanceof JThrowStmt) {
        PropertyGraph sootUpCPG = buildSootUpCPG(sootMethod);
        return WITUpGraph.fromPropertyGraph(sootUpCPG, sootMethod.getSignature().toString());
      }
    }
    throw new IllegalStateException("CPG must be available");
  }

  public PropertyGraph buildSootUpCPG(final JavaSootMethod m) {
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
