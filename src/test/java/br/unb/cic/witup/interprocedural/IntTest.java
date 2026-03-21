package br.unb.cic.witup.interprocedural;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.MethodSummariser;
import br.unb.cic.witup.analysis.MethodSummary;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntTest {
  Logger log = LoggerFactory.getLogger(IntTest.class);
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
            ProjectAnalyser.buildGraphsForClass(
                    new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Int").load());
  }
  
  @Test
  public void negateValue() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negateValue(int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    GraphRepository graphRepo = sig -> Optional.ofNullable(witupGraphs.get(sig));
    SummaryCache summaryCache = new SummaryCache();
    MethodSummariser methodSummariser = new MethodSummariser(cpg, graphRepo, summaryCache);

    List<List<SymbolicConstraint>> paths =
            methodSummariser.buildSymbolicConstraintPaths(throwNodes.get(0));

    SymbolicConstraintSolver solver = new SymbolicConstraintSolver(paths);
    List<SolverResult> results = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      results.add(solver.checkPath(methodSignature + "#" + i, paths.get(i)));
    }

    SolverResult sol0 = results.getFirst();
    assertTrue(sol0.isSat());
    assertTrue(sol0.getInt("a") > 0);
  }
}
