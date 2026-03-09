import br.unb.cic.witup.ProjectAnalyser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import br.unb.cic.witup.analysis.MethodAnalysis;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.SummaryGenerator;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Driver {
  private static final Path PROJECT_JARS_DIR = Path.of("./project-jars");
  private static final Logger log = LoggerFactory.getLogger("Driver");

  public static void main(final String[] args) {

    if (args.length != 1) {
      log.error("Usage: witup <jar-file>");
      System.exit(1);
    }

    Path jarPath = PROJECT_JARS_DIR.resolve(args[0]).normalize();

    if (!Files.exists(jarPath)) {
      log.error("Jar file not found: {}", jarPath);
      System.exit(1);
    }

    log.info("Starting analysis for {}", jarPath);

    ProjectAnalyser analyser = new ProjectAnalyser(jarPath);
    Map<String, WITUpGraph> methodGraphs = analyser.analyseProject();
    Map<String, MethodSummary> methodSummaries = new HashMap<>();
    for  (Map.Entry<String, WITUpGraph> methodGraph : methodGraphs.entrySet()) {
      MethodAnalysis analysis = new MethodAnalysis(methodGraph.getValue());
      SummaryGenerator summaryGenerator = new SummaryGenerator();
      MethodSummary summary = summaryGenerator.summarise(analysis);
      methodSummaries.put(methodGraph.getKey(), summary);
    }

    System.out.println(methodSummaries);

    log.info("Analysis completed");
  }

  public void solve() {
    log.debug("Solving constraint set");
  }
}
