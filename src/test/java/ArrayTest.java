import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.graph.WITUpAnalyser;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.edge.WITUpEdge;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.ThrowConditionSolver;
import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.ModelValue;
import com.microsoft.z3.IntExpr;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jgrapht.GraphPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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
    assertEquals(6, witupGraphs.size());
  }

  @Test
  public void getElement() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getElement(int[],int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    ThrowConditionSolver solver = new ThrowConditionSolver();
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.check(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    IntExpr indexExpr = sol0.getIntExpr("i");

    ModelValue elementValue = arrArray.get(indexExpr);
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void checkLength() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int checkLength(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    ThrowConditionSolver solver = new ThrowConditionSolver();
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.check(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());
    assertEquals(0, sol0.getInt("arr.length"));
  }

  @Test
  public void allocate() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] allocate(int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    ThrowConditionSolver solver = new ThrowConditionSolver();
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.check(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("n") < 0, "Expected n < 0");
  }

  @Test
  public void getStringElement() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: java.lang.String getStringElement(java.lang.String[],int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    ThrowConditionSolver solver = new ThrowConditionSolver();
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.check(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    IntExpr indexExpr = sol0.getIntExpr("i");

    ModelValue elementValue = arrArray.get(indexExpr);
    assertEquals("abc", elementValue.getString(), "arr[i] should be 0");
  }

  @Test
  public void getObjectElement() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: java.lang.Object getObjectElement(java.lang.Object[],int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    ThrowConditionSolver solver = new ThrowConditionSolver();
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.check(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    IntExpr indexExpr = sol0.getContext().mkInt(0);

    ModelValue elementValue = arrArray.get(indexExpr);
    assertEquals("abc", elementValue.getString(), "arr[i] should be 0");
  }

  @Test
  public void getObjectFromArray() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: br.unb.cic.witup.samples.Array$MyObject getObjectFromArray(br.unb.cic.witup.samples.Array$MyObject[],int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    ThrowConditionSolver solver = new ThrowConditionSolver();
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.check(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    IntExpr indexExpr = sol0.getIntExpr("i");

    ModelValue element = arrArray.get(indexExpr);
    System.out.println("element type: " + element.getClass() + " value: " + element);
    ModelValue fieldValue = element.getField("value");

    assertTrue(fieldValue.getInt() > 10, "expected arr[0].value <= 10");
  }
}
