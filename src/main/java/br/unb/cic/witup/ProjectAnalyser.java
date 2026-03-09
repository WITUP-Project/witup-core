package br.unb.cic.witup;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

public final class ProjectAnalyser {
  // assumes jar ia witup-core/project-jars
  private final Path jarPath;
  private static final Logger log = LoggerFactory.getLogger("ProjectAnalyser");

  public ProjectAnalyser(final Path jarPath) {
    this.jarPath = jarPath;
  }

  public void analyseProject() {
    AnalysisInputLocation inputLocation =
        new JavaClassPathAnalysisInputLocation(jarPath.toAbsolutePath().toString());
    JavaView view = new JavaView(inputLocation);
    List<JavaSootClass> classes = view.getClasses().toList();
    log.info("Found {} classes", classes.size());
    log.info(classes.toString());

    // for each class, analyse each method and
    // add get the method summaries. in intraprocedural
    // we do not need to share the CPGs yet.
//    classes.forEach(c -> {
//      new ClassAnalyser(c).analyseClass();
//    });
  }
}
