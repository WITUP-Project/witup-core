import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.MethodSummariser;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.SummaryCache;
import br.unb.cic.witup.analysis.graph.GraphRepository;
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
import java.util.Optional;
import org.jgrapht.GraphPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntTest {
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
        ProjectAnalyser.buildGraphsForClass(
            new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Int").load());
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(9, witupGraphs.size());
  }

  @Test
  public void addOverflow() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    WITUpGraph cpg = witupGraphs.get(methodSignature);

    MethodSummariser analysis = new MethodSummariser(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> symbolicConstraintPaths =
        analysis.buildSymbolicConstraintPaths(throwNodes.get(0));

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
      results.add(result);
    }
    SolverResult solution = results.getFirst();

    assertTrue(solution.isSat());
    int a = solution.getInt("a");
    int b = solution.getInt("b");
    assertTrue(a + b > 256, "Expected a + b > 256");

    MethodSummary summary = analysis.summarise();

    assertEquals(methodSignature, summary.getMethodSignature());
    assertEquals(1, summary.getSymbolicConstraintPaths().size());
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);

    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);

    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);

    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);

    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);

    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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

    SymbolicConstraintGenerator sg = new SymbolicConstraintGenerator(cpg, constraintPaths);

    List<List<SymbolicConstraint>> symbolicConstraintPaths = sg.generateSymbolicConstraintPaths();

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);

    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      SolverResult result = solver.checkPath(pathId, symbolicConstraintPaths.get(i));
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
    assertTrue(sol0.getInt("a") >= 0, "Expected a >= 0");

    // path 1 is unsat as
    // {"condition":"(a >= 0)","truthValue":false},{"condition":"(1 != 0)","truthValue":false}
    // is impossible
    SolverResult sol1 = results.get(1);
    assertTrue(sol1.isUnsat());
  }

  @Test
  public void addAndCheck() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    // wire repositories for interprocedural resolution
    GraphRepository graphRepo = sig -> Optional.ofNullable(witupGraphs.get(sig));
    SummaryCache summaryCache = new SummaryCache();

    MethodSummariser methodSummariser = new MethodSummariser(cpg, graphRepo, summaryCache);

    List<List<SymbolicConstraint>> symbolicConstraintPaths =
        methodSummariser.buildSymbolicConstraintPaths(throwNodes.get(0));

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(symbolicConstraintPaths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < symbolicConstraintPaths.size(); i++) {
      String pathId = methodSignature + "#" + i;
      results.add(solver.checkPath(pathId, symbolicConstraintPaths.get(i)));
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());
    int a = sol0.getInt("a");
    int b = sol0.getInt("b");
    assertTrue(a + b > 512, "Expected a + b > 512");
  }
}
