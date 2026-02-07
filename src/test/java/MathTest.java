import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.SymKind;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.solver.SolverInvoker;
import br.unb.cic.witup.solver.SolverSerialiser;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sootup.codepropertygraph.propertygraph.PropertyGraph;

// For now this is our basic test runner that will do an e2e run of sorts. We
// will need to break this up soon
public class MathTest {
  private Path testClassesDir;
  private SootUpAnalyser sootUpAnalyser;

  @BeforeEach
  void setUp() {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    testClassesDir = projectRoot.resolve("target/test-classes");
    sootUpAnalyser = new SootUpAnalyser();
  }

//  @Disabled
  @Test
  public void buildSootUpPropertyGraphs() {
    HashMap<String, SootUpPropertyGraphs> sootupGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

    assertNotNull(sootupGraphs);
    assertEquals(3, sootupGraphs.size());
  }

  @Test
  public void invalidField() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";
    SootUpPropertyGraphs circleAreaGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = circleAreaGraphs.getCPG();

    System.out.println(sootUpCPG);

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = circleAreaGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = circleAreaGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(throwConditionsPaths);
    Map<String, SymKind> symbolTypes = resolver.getSymbolTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);
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
  public void invalidParameter() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

    System.out.println(sootUpPropertyGraphs);

    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";
    SootUpPropertyGraphs invalidParameterGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = invalidParameterGraphs.getCPG();

    System.out.println(sootUpCPG);

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = invalidParameterGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = invalidParameterGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(throwConditionsPaths);
    Map<String, SymKind> symbolTypes = resolver.getSymbolTable();


    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);
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
  public void invalidParameterConjunction() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

    System.out.println(sootUpPropertyGraphs);

    String methodSignature =
        "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";
    SootUpPropertyGraphs invalidParameterConjunction = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = invalidParameterConjunction.getCPG();

    System.out.println(sootUpCPG);

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    PropertyGraph sootUpCFG = invalidParameterConjunction.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = invalidParameterConjunction.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(throwConditionsPaths);
    Map<String, SymKind> symbolTypes = resolver.getSymbolTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);
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
