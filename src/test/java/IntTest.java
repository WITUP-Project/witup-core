import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.SymKind;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertEquals(7, sootupGraphs.size());
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

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);
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

      assertTrue(aValue + bValue > 256 , "Expected a + b > 256");
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
    SootUpPropertyGraphs sootupGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootupGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootupGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = sootupGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);
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
    SootUpPropertyGraphs circleAreaGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = circleAreaGraphs.getCPG();

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
    SootUpPropertyGraphs sootupGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootupGraphs.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootupGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = sootupGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);
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

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);
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

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);
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

    String methodSignature = "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";
    SootUpPropertyGraphs sootUpGraphs = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = sootUpGraphs.getCPG();

    String dot = sootUpCPG.toDotGraph();
    try {
      Graphviz.fromString(dot)
              .render(Format.SVG)
              .toFile(new File("int-check-via-boolean.svg"));
    } catch (IOException ignored) {

    }

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    PropertyGraph sootUpCFG = sootUpGraphs.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = sootUpGraphs.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);
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
}
