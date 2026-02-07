import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.solver.SolverInvoker;
import br.unb.cic.witup.solver.SolverSerialiser;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.codepropertygraph.propertygraph.PropertyGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TextTest {
  private Path testClassesDir;
  private SootUpAnalyser sootUpAnalyser;

  @BeforeEach
  void setUp() {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    testClassesDir = projectRoot.resolve("target/test-classes");
    sootUpAnalyser = new SootUpAnalyser();
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    HashMap<String, SootUpPropertyGraphs> sootupGraphs =
            sootUpAnalyser.analyseThrowingMethods(
                    testClassesDir.toString(), "br.unb.cic.witup.samples.Text");

    assertNotNull(sootupGraphs);
    assertEquals(2, sootupGraphs.size());
  }

  @Test
  public void invalidString() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
            sootUpAnalyser.analyseThrowingMethods(
                    testClassesDir.toString(), "br.unb.cic.witup.samples.Text");

    System.out.println(sootUpPropertyGraphs);

    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";
    SootUpPropertyGraphs invalidString = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = invalidString.getCPG();

    System.out.println(sootUpCPG);
    String dot = sootUpCPG.toDotGraph();

    try {
      Graphviz.fromString(dot)
              .render(Format.SVG)
              .toFile(new File("invalid-string-cpg.svg"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);

    PropertyGraph sootUpCFG = invalidString.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = invalidString.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths);
    System.out.println(request);

    String pythonScript =
            Paths.get(System.getProperty("user.dir"))
                    .resolve("src/main/solver/solver.py")
                    .toAbsolutePath()
                    .toString();
    SolverInvoker si = new SolverInvoker(pythonScript);
    try {
      String resp = si.callSolver(request);
      System.out.println(resp);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidStringLength() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
            sootUpAnalyser.analyseThrowingMethods(
                    testClassesDir.toString(), "br.unb.cic.witup.samples.Text");

    System.out.println(sootUpPropertyGraphs);

    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    SootUpPropertyGraphs invalidString = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = invalidString.getCPG();

    System.out.println(sootUpCPG);
    String dot = sootUpCPG.toDotGraph();

    try {
      Graphviz.fromString(dot)
              .render(Format.SVG)
              .toFile(new File("invalid-string-length-cpg.svg"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);

    PropertyGraph sootUpCFG = invalidString.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = invalidString.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths);
    System.out.println(request);

    String pythonScript =
            Paths.get(System.getProperty("user.dir"))
                    .resolve("src/main/solver/solver.py")
                    .toAbsolutePath()
                    .toString();
    SolverInvoker si = new SolverInvoker(pythonScript);
    try {
      String resp = si.callSolver(request);
      System.out.println(resp);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
