package br.unb.cic.witup.interprocedural;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraintGenerator;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
import br.unb.cic.witup.solver.model.BoolValue;
import br.unb.cic.witup.solver.model.IntValue;
import org.jgrapht.GraphPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TextTest {
  Logger log = LoggerFactory.getLogger(IntTest.class);
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
            ProjectAnalyser.buildGraphsForClass(
                    new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Text").load());
  }

  @Test
  public void invalidStringLength() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidStringLength(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

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
    assertEquals(new IntValue(0), sol0.modelValueMap().get("s.length()"));
  }

  @Test
  public void invalidEmptyString() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Text: boolean invalidEmptyString(java.lang.String)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();

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
    assertEquals(new BoolValue(true), sol0.modelValueMap().get("s.isEmpty()"));
  }
}
