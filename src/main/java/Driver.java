import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SolverResultDTO;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Driver {
  private static final Path PROJECT_JARS_DIR = Path.of("./project-jars");
  private static final Logger log = LoggerFactory.getLogger("Driver");

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
    Map<String, MethodSummary> methodSummaries = analyser.summariseAll(methodGraphs);
    // soot land stops here. From now on only SymbolicConstraints feed into the
    // solver
    SymbolicConstraintSolver s = new SymbolicConstraintSolver(methodSummaries);
    Map<String, List<SolverResult>> methodSolutions = s.solveConstraints();
    System.out.println(methodSolutions);

    Map<String, List<SolverResultDTO>> dtoSolutions =
        methodSolutions.entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        entry.getValue().stream()
                            .map(SolverResultDTO::from)
                            .collect(Collectors.toList())));

    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(Path.of("witup-results.json").toFile(), dtoSolutions);

    log.info("Analysis completed");
  }

  public void solve() {
    log.debug("Solving constraint set");
  }
}
