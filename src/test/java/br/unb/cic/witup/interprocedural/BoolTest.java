package br.unb.cic.witup.interprocedural;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.MethodSummariser;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.SummaryCache;
import br.unb.cic.witup.analysis.graph.GraphRepository;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.solver.SolverResult;
import br.unb.cic.witup.solver.SymbolicConstraintSolver;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BoolTest {
  Logger log = LoggerFactory.getLogger(IntTest.class);
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
            ProjectAnalyser.buildGraphsForClass(
                    new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Bool").load());
  }

  @Test
  public void toBoolean() {
    String methodSignature = "<br.unb.cic.witup.samples.Bool: boolean toBoolean(java.lang.Integer,java.lang.Integer,java.lang.Integer)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(5, conditionNodes.size());

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

    SolverResult sol0 = results.get(0);
    assertTrue(sol0.isSat());
    assertFalse(sol0.getBool("value_is_null"));
    assertFalse(sol0.getBool("value.equals(trueValue)"));
    assertFalse(sol0.getBool("value.equals(falseValue)"));

    SolverResult sol1 = results.get(1);
    assertTrue(sol1.isSat());
    assertTrue(sol1.getBool("value_is_null"));
    assertFalse(sol1.getBool("trueValue_is_null"));
    assertFalse(sol1.getBool("falseValue_is_null"));
  }
}
