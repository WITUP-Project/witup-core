import br.unb.cic.witup.ProjectAnalyser;
import java.nio.file.Files;
import java.nio.file.Path;
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

    log.info("Analysis completed");
  }

  public void solve() {
    log.debug("Solving constraint set");
  }
}
