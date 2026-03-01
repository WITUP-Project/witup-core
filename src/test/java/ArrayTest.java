import br.unb.cic.witup.analysis.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymKind;
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
import org.jgrapht.GraphPath;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArrayTest {
  HashMap<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    Path testClassesDir = projectRoot.resolve("target/test-classes");
    WITUpAnalyser witUpAnalyser =
            new WITUpAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Array");
    witupGraphs = witUpAnalyser.buildWitUpGraphs();
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(3, witupGraphs.size());
  }

  @Test
  public void getElement() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getElement(int[],int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConstraintNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements =
            cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator resolver = new BackwardSymbolicGenerator(cpg, pathsWithIfStatements);

    List<List<SymbolicConstraint>> resolvedConditionPaths = resolver.generateSymbolicConstraintPaths();

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

      int arrayElementValue = SolverResponseAssertions.intValue(p0, "arr[i]");

      assertEquals(0, arrayElementValue, "Expected arr[i] == 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void checkLength() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int checkLength(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConstraintNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements =
            cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator lifter = new BackwardSymbolicGenerator(cpg, pathsWithIfStatements);

    List<List<SymbolicConstraint>> resolvedConditionPaths = lifter.generateSymbolicConstraintPaths();

    Map<String, SymKind> symbolTypes = lifter.getSymbolKindTable();

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

      int arrayElementValue = SolverResponseAssertions.intValue(p0, "arr.length");

      assertEquals(0, arrayElementValue, "Expected arr.length == 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void allocate() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] allocate(int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConstraintNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
            cpg.getConstraintPaths(throwNodes.get(0));

    // constraints from WITUpGraph (Jimple inside) -> Symbolic constraints
    // with backwards data flow symbolic resolution. Not happy with this name
    // the sematics is to grab the constraints that need to be satisfied for
    // to arrive at a throw. we do this becaues then path explosion gets
    // insanely mitigated. What is the name of this "PathResolver"
    BackwardSymbolicGenerator lifter = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = lifter.generateSymbolicConstraintPaths();

    Map<String, SymKind> symbolKinds = lifter.getSymbolKindTable();

    SolverSerialiser serialiser = new SolverSerialiser(methodSignature);
    JSONObject request = serialiser.serializeResolvedPaths(symbolicConstraintPaths, symbolKinds);

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

      int sizeValue = SolverResponseAssertions.intValue(p0, "n");

      assertTrue(sizeValue < 0, "Expected n < 0");
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
