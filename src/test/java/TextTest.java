import static org.junit.jupiter.api.Assertions.*;

import br.unb.cic.witup.analysis.PathResolver;
import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymKind;
import br.unb.cic.witup.graph.WITUpAnalyser;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.solver.SolverInvoker;
import br.unb.cic.witup.solver.SolverResponse;
import br.unb.cic.witup.solver.SolverResponseAssertions;
import br.unb.cic.witup.solver.SolverSerialiser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TextTest {
  HashMap<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    Path testClassesDir = projectRoot.resolve("target/test-classes");
    WITUpAnalyser witUpAnalyser =
        new WITUpAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Text");
    witupGraphs = witUpAnalyser.buildWitUpGraphs();
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(3, witupGraphs.size());
  }

  @Test
  public void invalidString() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements =
        cpg.getPathsWithIfStatements(throwNodes.get(0));

    PathResolver resolver = new PathResolver(cpg, pathsWithIfStatements);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths = resolver.resolveConditionPaths();

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

      String stringValue = SolverResponseAssertions.stringValue(p0, "s");
      assertEquals("abc", stringValue);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidStringLength() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements =
        cpg.getPathsWithIfStatements(throwNodes.get(0));

    PathResolver resolver = new PathResolver(cpg, pathsWithIfStatements);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths = resolver.resolveConditionPaths();

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

      int lengthValue = SolverResponseAssertions.intValue(p0, "s.length");
      assertEquals(0, lengthValue, "Expected length 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidEmptyString() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidEmptyString(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements =
        cpg.getPathsWithIfStatements(throwNodes.get(0));

    PathResolver resolver = new PathResolver(cpg, pathsWithIfStatements);

    List<List<ResolvedThrowCondition>> resolvedConditionPaths = resolver.resolveConditionPaths();

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

      boolean truthValue = SolverResponseAssertions.booleanValue(p0, "s.isEmpty");
      assertTrue(truthValue, "Expected s.isEmpty to be true");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
