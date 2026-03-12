import br.unb.cic.witup.analysis.MethodConstraintAnalysis;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SolverResultDTO;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Driver {
  private static final Path PROJECT_JARS_DIR = Path.of("./project-jars");
  private static final Logger log = LoggerFactory.getLogger("Driver");

  private Driver() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static void main(final String[] args) throws IOException {

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

    Map<String, MethodSummary> methodSummaries = new LinkedHashMap<>();
    Map<String, String> failures = new LinkedHashMap<>();

    for (Map.Entry<String, WITUpGraph> entry : methodGraphs.entrySet()) {
      String sig = entry.getKey();
      try {
        methodSummaries.put(sig,
                new MethodConstraintAnalysis(entry.getValue()).summariseConstraintPaths());
      } catch (Exception e) {
        log.warn("Failed to summarise {}: {}", sig, e.getMessage());
        failures.put(sig, e.getClass().getSimpleName() + ": " + e.getMessage());
      }
    }

    log.info("Summarised {}/{} methods", methodSummaries.size(), methodGraphs.size());

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(methodSummaries);
    Map<String, List<SolverResult>> methodSolutions = solver.solveConstraintsSafe(failures);

    Map<String, List<SolverResultDTO>> dtoSolutions = methodSolutions.entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                            .map(SolverResultDTO::from)
                            .collect(Collectors.toList())));

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(Path.of("witup-results.json").toFile(), dtoSolutions);
    mapper.writeValue(Path.of("witup-failures.json").toFile(), failures);

    log.info("Results written to witup-results.json ({} methods)", dtoSolutions.size());
    log.info("Failures written to witup-failures.json ({} methods)", failures.size());
  }
}
