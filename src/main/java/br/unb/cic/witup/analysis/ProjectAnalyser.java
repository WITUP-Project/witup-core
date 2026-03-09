package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.CPGBuilder;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JThrowStmt;
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
    return buildGraphsForClass(sootClass);
  }

  public static Map<String, WITUpGraph> buildGraphsForClass(final JavaSootClass sootClass) {
    return sootClass.getMethods().stream()
        .filter(JavaSootMethod::hasBody)
        .filter(ProjectAnalyser::methodHasThrow)
        .collect(Collectors.toMap(m -> m.getSignature().toString(), CPGBuilder::buildForMethod));
  }

  private static boolean methodHasThrow(final JavaSootMethod method) {
    return method.getBody().getStmtGraph().getNodes().stream()
        .anyMatch(s -> s instanceof JThrowStmt);
  }

  public Map<String, MethodSummary> summariseAll(final Map<String, WITUpGraph> methodGraphs) {
    return methodGraphs.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                    new MethodConstraintAnalysis(entry.getValue()).summariseConstraintPaths()));
  }
}
