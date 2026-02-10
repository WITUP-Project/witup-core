import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.SymKind;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import sootup.codepropertygraph.propertygraph.PropertyGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
    // This has to equal the number o methods in the class that throw
    assertEquals(3, sootupGraphs.size());
  }

  @Test
  public void invalidString() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
            sootUpAnalyser.analyseThrowingMethods(
                    testClassesDir.toString(), "br.unb.cic.witup.samples.Text");

    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";
    SootUpPropertyGraphs invalidString = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = invalidString.getCPG();

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
      String jsonString = si.callSolver(request);
      ObjectMapper mapper = new ObjectMapper();

      SolverResponse response =
              mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
              SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      String stringValue = SolverResponseAssertions.stringValue(p0, "s");
      assertEquals("abc", stringValue);
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
    Map<String, SymKind> symbolTypes = resolver.getSymbolTable();


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

      SolverResponse response =
              mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
              SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      int lengthValue = SolverResponseAssertions.intValue(p0, "s.length");
      assertEquals(0, lengthValue, "Expected length 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidEmptyString() {
    HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs =
            sootUpAnalyser.analyseThrowingMethods(
                    testClassesDir.toString(), "br.unb.cic.witup.samples.Text");

    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidEmptyString(java.lang.String)>";
    SootUpPropertyGraphs invalidEmptyString = sootUpPropertyGraphs.get(methodSignature);
    PropertyGraph sootUpCPG = invalidEmptyString.getCPG();

    WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

    List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);

    PropertyGraph sootUpCFG = invalidEmptyString.getCFG();
    WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

    List<List<ThrowCondition>> throwConditionsPaths =
            WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

    PropertyGraph sootUpDDG = invalidEmptyString.getDDG();
    WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

    Resolver resolver = new Resolver(witUpDDG);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths =
            resolver.resolveConditionPaths(throwConditionsPaths);

    Map<String, SymKind> symbolTypes = resolver.getSymbolTable();

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

      SolverResponse response =
              mapper.readValue(jsonString, SolverResponse.class);

      SolverResponse.SolverPathResult p0 =
              SolverResponseAssertions.path(response, methodSignature + "#0");

      assertEquals(SolverResponse.Status.SAT, p0.getStatus());

      boolean truthValue = SolverResponseAssertions.booleanValue(p0, "s.isEmpty");
      assertTrue(truthValue, "Expected s.isEmpty to be true");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
