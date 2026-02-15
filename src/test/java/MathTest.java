import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.SymKind;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.solver.SolverInvoker;
import br.unb.cic.witup.solver.SolverResponse;
import br.unb.cic.witup.solver.SolverResponseAssertions;
import br.unb.cic.witup.solver.SolverSerialiser;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
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
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.getThrowNodes();
    assertEquals(1, throwNodes.size());

    // for each throw node, we are gonna need to get the respective conditions
    List<WITUpNode> conditionNodes =
        witUpCPG.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        witUpCFG.getPathsWithIfStatements(throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        witUpCPG.getThrowConditionsPaths(pathsWithConditions);

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpCPG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(pathsWithConditions, throwConditionsPaths);

    Map<String, SymKind> symbolTypes = resolver.getSymbolKindTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);

    String pythonScript =
        Paths.get(System.getProperty("user.dir"))
            .resolve("src/main/solver/solver.py")
            .toAbsolutePath()
            .toString();
    SolverInvoker si = new SolverInvoker(pythonScript);
    try {
      String jsonString = si.callSolver(request);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int radiusValue = SolverResponseAssertions.intValue(p0, "this.radius");
      assertTrue(radiusValue < 0, "Expected radius <= 0");
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
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    System.out.println(sootUpCPG);

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        witUpCPG.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        witUpCFG.getPathsWithIfStatements(throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        witUpCPG.getThrowConditionsPaths(pathsWithConditions);

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpCPG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(pathsWithConditions, throwConditionsPaths);

    Map<String, SymKind> symbolTypes = resolver.getSymbolKindTable();

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
      String jsonString = si.callSolver(request);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int yValue = SolverResponseAssertions.intValue(p0, "y");
      assertEquals(0, yValue, "Expected y == 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidParameterConjunction() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

    String methodSignature =
        "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        witUpCPG.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        witUpCFG.getPathsWithIfStatements(throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        witUpCPG.getThrowConditionsPaths(pathsWithConditions);

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpCPG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(pathsWithConditions, throwConditionsPaths);

    Map<String, SymKind> symbolTypes = resolver.getSymbolKindTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);

    String pythonScript =
        Paths.get(System.getProperty("user.dir"))
            .resolve("src/main/solver/solver.py")
            .toAbsolutePath()
            .toString();
    SolverInvoker si = new SolverInvoker(pythonScript);
    try {
      String jsonString = si.callSolver(request);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int pValue0 = SolverResponseAssertions.intValue(p0, "p");
      assertTrue(pValue0 < 0, "Expected p to be negative");

      SolverResponse.SolverPathResult p1 =
          SolverResponseAssertions.path(response, methodSignature + "#1");

      assertEquals(SolverResponse.Status.SAT, p1.getStatus());

      int pValue1 = SolverResponseAssertions.intValue(p1, "p");
      assertTrue(pValue1 > 1, "Expected p > 1");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
