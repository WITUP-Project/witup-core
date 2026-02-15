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
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import java.io.File;
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

public class IntTest {
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
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    assertNotNull(sootupGraphs);
    assertEquals(8, sootupGraphs.size());
  }

  @Test
  public void addOverflow() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

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
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int aValue = SolverResponseAssertions.intValue(p0, "a");
      int bValue = SolverResponseAssertions.intValue(p0, "b");

      assertTrue(aValue + bValue > 256, "Expected a + b > 256");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void greaterThanConstantRhs() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

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
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int aValue = SolverResponseAssertions.intValue(p0, "a");

      assertTrue(aValue < 0, "a < 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void lesserThanConstantLhs() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

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
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int aValue = SolverResponseAssertions.intValue(p0, "a");

      assertTrue(aValue < 0, "Expected a < 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void equalsConstantRhs() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

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
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int aValue = SolverResponseAssertions.intValue(p0, "a");

      assertEquals(0, aValue, "Expected a == 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void equalsConstantLhs() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

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
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int aValue = SolverResponseAssertions.intValue(p0, "a");

      assertEquals(0, aValue, "Expected a == 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void negatedLessThanConstantRhs() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

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
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int aValue = SolverResponseAssertions.intValue(p0, "a");

      assertTrue(aValue <= 0, "Expected a <= 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void lessThanConstantRhsViaBoolean() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
        sootUpAnalyser.analyseThrowingMethods(
            testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    String dot = sootUpCPG.toDotGraph();
    try {
      Graphviz.fromString(dot).render(Format.SVG).toFile(new File("int-check-via-boolean.svg"));
    } catch (IOException ignored) {

    }

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
        WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
        WITUpGraph.findContitionPaths(pathsWithConditions);

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpCPG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
        resolver.resolveConditionPaths(pathsWithConditions, throwConditionsPaths);

    Map<String, SymKind> symbolTypes = resolver.getSymbolKindTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);

    System.out.println(request.toString());

    String pythonScript =
        Paths.get(System.getProperty("user.dir"))
            .resolve("src/main/solver/solver.py")
            .toAbsolutePath()
            .toString();
    SolverInvoker si = new SolverInvoker(pythonScript);
    try {
      String jsonString = si.callSolver(request);
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);

      // path 0 is unsat as
      // {"condition":"(a >= 0)","truthValue":true},{"condition":"(0 == 0)","truthValue":false}
      // is impossible
      SolverResponse.SolverPathResult p0 =
          SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.UNSAT, p0.getStatus());

      SolverResponse.SolverPathResult p1 =
          SolverResponseAssertions.path(response, methodSignature + "#1");

      assertEquals(SolverResponse.Status.SAT, p1.getStatus());

      int aValue = SolverResponseAssertions.intValue(p1, "a");
      assertTrue(aValue < 0, "Expected a < 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void lessThanConstantRhsViaNegatedBoolean() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
            sootUpAnalyser.analyseThrowingMethods(
                    testClassesDir.toString(), "br.unb.cic.witup.samples.Int");

    String methodSignature =
            "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    String dot = sootUpCPG.toDotGraph();
    try {
      Graphviz.fromString(dot).render(Format.SVG).toFile(new File("int-check-via-neg-boolean.svg"));
    } catch (IOException ignored) {

    }

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = witUpCPG.findThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConditions =
            WITUpGraph.findPathsWithConditions(witUpCFG, throwNodes.get(0));

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findContitionPaths(pathsWithConditions);

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpCPG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(pathsWithConditions, throwConditionsPaths);

    Map<String, SymKind> symbolTypes = resolver.getSymbolKindTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(resolvedConditionPaths, symbolTypes);

    System.out.println(request.toString());

    String pythonScript =
            Paths.get(System.getProperty("user.dir"))
                    .resolve("src/main/solver/solver.py")
                    .toAbsolutePath()
                    .toString();
    SolverInvoker si = new SolverInvoker(pythonScript);
    try {
      String jsonString = si.callSolver(request);
      System.out.println(jsonString);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response = mapper.readValue(jsonString, SolverResponse.class);


      SolverResponse.SolverPathResult p0 =
              SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());
      int aValue = SolverResponseAssertions.intValue(p0, "a");
      assertTrue(aValue >= 0, "Expected a >= 0");

      // path 1 is unsat as
      // {"condition":"(a >= 0)","truthValue":false},{"condition":"(1 != 0)","truthValue":false}
      // is impossible
      SolverResponse.SolverPathResult p1 =
              SolverResponseAssertions.path(response, methodSignature + "#1");

      assertEquals(SolverResponse.Status.UNSAT, p1.getStatus());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
