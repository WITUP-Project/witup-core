import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.PathResolver;
import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymKind;
import br.unb.cic.witup.graph.WITUpAnalyser;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
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
public class MathTest {
  HashMap<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    Path testClassesDir = projectRoot.resolve("target/test-classes");
    WITUpAnalyser witUpAnalyser =
        new WITUpAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Math");
    witupGraphs = witUpAnalyser.buildWitUpGraphs();
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(3, witupGraphs.size());
  }

  @Test
  public void invalidField() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    // for each throw node, we are gonna need to get the respective conditions
    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

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

      int radiusValue = SolverResponseAssertions.intValue(p0, "this.radius");
      assertTrue(radiusValue < 0, "Expected radius <= 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidParameter() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

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

      int yValue = SolverResponseAssertions.intValue(p0, "y");
      assertEquals(0, yValue, "Expected y == 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void invalidParameterConjunction() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

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
