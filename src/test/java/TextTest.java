import static org.junit.jupiter.api.Assertions.*;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.BackwardSymbolicGenerator;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import br.unb.cic.witup.solver.model.BoolValue;
import br.unb.cic.witup.solver.model.IntValue;
import br.unb.cic.witup.solver.model.StringValue;
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
public class TextTest {
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
        ProjectAnalyser.buildGraphsForClass(
            new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Text").load());
  }

  @Test
  public void buildSootUpPropertyGraphs() {
    assertNotNull(witupGraphs);
    assertEquals(4, witupGraphs.size());
  }

  @Test
  public void invalidString() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidString(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

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
    assertEquals(new StringValue("abc"), sol0.modelValueMap().get("s"));
  }

  @Test
  public void invalidStringLength() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

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
    assertEquals(new IntValue(0), sol0.modelValueMap().get("s.length"));
  }

  @Test
  public void invalidEmptyString() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: boolean invalidEmptyString(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

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
    assertEquals(new BoolValue(true), sol0.modelValueMap().get("s.isEmpty"));
  }

  @Test
  public void requireString() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Text: java.lang.String requireString(java.lang.Object)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

    List<GraphPath<WITUpNode, WITUpEdge>> constraintPaths =
        cpg.getConstraintPaths(throwNodes.get(0));

    BackwardSymbolicGenerator sg = new BackwardSymbolicGenerator(cpg, constraintPaths);

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
    assertFalse(sol0.getBool("s_instanceof_java_lang_String"));
  }
}
