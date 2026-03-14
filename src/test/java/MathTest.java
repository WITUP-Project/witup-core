import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class MathTest {
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
        ProjectAnalyser.buildGraphsForClass(
            new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Math").load());
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(5, witupGraphs.size());
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

    SolverResult sol = results.getFirst();
    assertTrue(sol.isSat());
    assertTrue(sol.getInt("this.radius") < 0, "Expected radius <= 0");
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

    SolverResult sol = results.getFirst();
    assertTrue(sol.isSat());
    assertEquals(0, sol.getInt("y"), "Expected y == 0");
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
    assertTrue(sol0.getInt("p") < 0, "Expected p < 0");

    SolverResult sol1 = results.get(1);
    assertTrue(sol1.isSat());
    assertTrue(sol1.getInt("p") > 1, "Expected p > 1");
  }

  @Test
  public void truncate() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncate(double)>";
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
    assertTrue(sol0.getInt("truncated") < 0, "Expected truncated < 0");
  }

  @Test
  public void truncateInline() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncateInline(double)>";
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
    assertTrue(sol0.getInt("(int)d") < 0, "Expected (int)d\" < 0");
  }
}
