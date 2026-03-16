package br.unb.cic.witup;

import br.unb.cic.witup.analysis.MethodParts;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Driver {
  private static final Path PROJECT_JARS_DIR = Path.of("./project-jars");
  private static final Path PROJECT_RESULTS = Path.of("./project-results");
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

    Map<String, String> failures = new LinkedHashMap<>();
    Map<String, MethodSummary> methodSummaries = analyser.summariseAll(methodGraphs, failures);

    log.info("Summarised {}/{} methods", methodSummaries.size(), methodGraphs.size());

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(methodSummaries);
    Map<String, List<SolverResult>> methodSolutions = solver.solveConstraintsSafe(failures);

    List<Map<String, Object>> rows = new ArrayList<>();

    // build something pandas/dataframe friendly
    for (var entry : methodSolutions.entrySet()) {
      MethodParts parts = MethodParts.parseSignature(entry.getKey());
      for (SolverResult r : entry.getValue()) {
        Map<String, Object> row = new LinkedHashMap<>();

        String pathId = r.getPathId();
        int idx = pathId.lastIndexOf('#');
        int pathIndex = Integer.parseInt(pathId.substring(idx + 1));

        row.put("artifact", args[0]);
        row.put("package", parts.pkg());
        row.put("class", parts.clazz());
        row.put("method", parts.method());
        row.put("returnType", parts.returnType());
        row.put("params", parts.params());
        row.put("pathIndex", pathIndex);
        row.put("pathId", pathId);
        row.put("status", r.getStatus().toString());
        row.put("modelValues", r.getModelValueMap());
        rows.add(row);
      }
    }

    String projectName = args[0].replaceFirst("\\.jar$", "");
    Path projectResultsDir = PROJECT_RESULTS.resolve(projectName);
    Files.createDirectories(projectResultsDir);

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);

    mapper.writeValue(projectResultsDir.resolve("witup-results.json").toFile(), rows);

    mapper.writeValue(projectResultsDir.resolve("witup-failures.json").toFile(), failures);

    log.info("Results written to witup-results.json ({} methods)", methodSolutions.size());
    log.info("Failures written to witup-failures.json ({} methods)", failures.size());
  }
}
