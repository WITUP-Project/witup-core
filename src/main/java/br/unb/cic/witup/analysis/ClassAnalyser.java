package br.unb.cic.witup.analysis;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

/**
 * Entry point of the analysis pipeline. Analyses a class given its location and name. For each
 * method to be analysed, build the individual graphs and the resulting Code Property Graph (CPG)
 */
public final class ClassAnalyser {
  private final String location;
  private final String className;

  public ClassAnalyser(final String location, final String className) {
    this.location = location;
    this.className = className;
  }

  public JavaSootClass load() {
    AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(location);
    JavaView view = new JavaView(inputLocation);
    JavaClassType classType = view.getIdentifierFactory().getClassType(className);
    return view.getClass(classType)
        .orElseThrow(() -> new RuntimeException("Soot class not found: " + classType));
  }
}
