package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.CPGBuilder;
import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public final class ProjectAnalyser implements GraphRepository {
  // assumes jar ia witup-core/project-jars
  private final Path jarPath;
  private static final Logger log = LoggerFactory.getLogger("ProjectAnalyser");
  private final Map<String, WITUpGraph> methodGraphs = new HashMap<>();
  private final SummaryCache summaryCache = new SummaryCache();

  public ProjectAnalyser(final Path jarPath) {
    this.jarPath = jarPath;
  }

  @Override
  public Optional<WITUpGraph> getGraph(final String methodSignature) {
    return Optional.ofNullable(methodGraphs.get(methodSignature));
  }

  public Map<String, MethodSummary> summariseAll(final Map<String, WITUpGraph> graphs) {
    Map<String, MethodSummary> summaries = new LinkedHashMap<>();
    for (Map.Entry<String, WITUpGraph> entry : graphs.entrySet()) {
      String sig = entry.getKey();
      try {
        MethodSummary summary =
            new MethodConstraintAnalysis(entry.getValue(), this, summaryCache)
                .summariseConstraintPaths();
        summaries.put(sig, summary);
      } catch (Exception e) {
        log.warn("Failed to summarise {}: {}", sig, e.getMessage());
      }
    }
    return summaries;
  }

  public Map<String, WITUpGraph> analyseProject() {
    AnalysisInputLocation inputLocation =
        new JavaClassPathAnalysisInputLocation(jarPath.toAbsolutePath().toString());
    JavaView view = new JavaView(inputLocation);
    List<JavaSootClass> classes = view.getClasses().toList();
    log.info("Found {} classes", classes.size());
    log.info(classes.toString());

    Map<String, WITUpGraph> methodGraph = analyseClasses(classes);
    methodGraphs.putAll(methodGraph);
    return methodGraphs;
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
}
