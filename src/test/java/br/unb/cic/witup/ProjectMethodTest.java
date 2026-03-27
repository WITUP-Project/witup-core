package br.unb.cic.witup;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;

public class ProjectMethodTest {
  private static final Path PROJECT_JARS_DIR = Path.of("./project-jars");

  // use this to run a specific method from a jar
  @Disabled
  public void projectMethodTest() {
    Path jarPath = PROJECT_JARS_DIR.resolve("commons-io-11f0abe7a.jar").normalize();
    ProjectAnalyser analyser = new ProjectAnalyser(jarPath);
    System.out.println("analysing method");
    String methodSignature =
        "<org.apache.commons.io.FileSystemUtils: long freeSpaceWindows(java.lang.String,long)>";
    AnalysisResult analysis = analyser.analyseSingleMethod(methodSignature);
  }
}
