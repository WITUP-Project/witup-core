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
public class IntTest {
  HashMap<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    Path testClassesDir = projectRoot.resolve("target/test-classes");
    WITUpAnalyser witUpAnalyser =
        new WITUpAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Int");
    witupGraphs = witUpAnalyser.buildWitUpGraphs();
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(8, witupGraphs.size());
  }

  @Test
  public void addOverflow() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

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

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    int a = solution.getInt("a");
    int b = solution.getInt("b");
    assertTrue(a + b > 256, "Expected a + b > 256");
  }

  @Test
  public void greaterThanConstantRhs() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";

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

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") < 0, "a < 0");
  }

  @Test
  public void lesserThanConstantLhs() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";
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

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") < 0, "Expected a < 0");
  }

  @Test
  public void equalsConstantRhs() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";
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

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    assertEquals(0, solution.getInt("a"), "Expected a == 0");
  }

  @Test
  public void equalsConstantLhs() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";
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

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    assertEquals(0, solution.getInt("a"), "Expected a == 0");
  }

  @Test
  public void negatedLessThanConstantRhs() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
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

    SolverResult solution = results.getFirst();
    assertTrue(solution.isSat());
    assertTrue(solution.getInt("a") <= 0, "Expected a <= 0");
  }

  @Test
  public void lessThanConstantRhsViaBoolean() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

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

    // path 0 is unsat as
    // {"condition":"(a >= 0)","truthValue":true},{"condition":"(0 == 0)","truthValue":false}
    // is impossible
    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isUnsat());

    SolverResult sol1 = results.get(1);
    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("a") < 0, "Expected a < 0");
  }

  @Test
  public void lessThanConstantRhsViaNegatedBoolean() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());

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
    assertTrue(sol0.getInt("a") >= 0, "Expected a >= 0");

    // path 1 is unsat as
    // {"condition":"(a >= 0)","truthValue":false},{"condition":"(1 != 0)","truthValue":false}
    // is impossible
    SolverResult sol1 = results.get(1);
    assertTrue(sol1.isUnsat());
  }
}
