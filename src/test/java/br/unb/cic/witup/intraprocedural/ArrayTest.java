package br.unb.cic.witup.intraprocedural;

import static br.unb.cic.witup.testinfra.SymbolicTestHelper.solve;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraintGenerator;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.ModelValue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jgrapht.GraphPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArrayTest {
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
        ProjectAnalyser.buildGraphsForClass(
            new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Array").load());
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(13, witupGraphs.size());
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get(0);
    assertEquals("abc", elementValue.getString(), "arr[0] should be 0");
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    System.out.println("element type: " + elementValue.getClass() + " value: " + elementValue);
    ModelValue fieldValue = elementValue.getField("value");

    assertTrue(fieldValue.getInt() > 10, "expected arr[0].value <= 10");
  }

  @Test
  public void sumUntilZero() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZero(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void sumUntilZeroWhile() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroWhile(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void sumUntilZeroDoWhile() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroDoWhile(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void sumUntilZeroForEach() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroForEach(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    ArrayValue arrArray = sol0.getArray("arr");
    ModelValue elementValue = arrArray.get("i");
    assertEquals(0, elementValue.getInt(), "arr[i] should be 0");
  }

  @Test
  public void requireNonNullObject() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Array: void requireNonNullObject(java.lang.Object)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    assertEquals(0, sol0.getInt("o"), "o to be 0 (encoding for null)");
  }

  @Test
  public void requireNonNullArray() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] requireNonNullArray(int[])>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());

    assertTrue(sol0.getBool("arr_is_null"), "arr_is_null to be true");
  }

  @Test
  public void getChecked() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getChecked(int[],int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(3, throwNodes.size());

    // first throw node (null check)
    List<WITUpNode> conditionNodes0 =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes0.size());

    SolverResult sol0 = solve(cpg, throwNodes.get(0), methodSignature).getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getBool("arr_is_null"), "arr must be null");

    // second throw node (i < 0)
    List<WITUpNode> conditionNodes1 =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(1));
    assertEquals(2, conditionNodes1.size());

    SolverResult sol1 = solve(cpg, throwNodes.get(1), methodSignature).getFirst();
    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("i") < 0, "expected i < 0");

    // third throw node (i >= arr.length)
    List<WITUpNode> conditionNodes2 =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(2));
    assertEquals(3, conditionNodes2.size());

    SolverResult sol2 = solve(cpg, throwNodes.get(2), methodSignature).getFirst();
    assertTrue(sol2.isSat());
    assertFalse(sol2.getBool("arr_is_null"), "arr must not be null");
    assertTrue(sol2.getInt("i") >= 0, "i must be non-negative");
    assertTrue(sol2.getInt("i") >= sol2.getInt("arr.length"), "expected i >= arr.length");
  }
}
