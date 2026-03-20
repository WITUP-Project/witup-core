package br.unb.cic.witup.graph;

import br.unb.cic.witup.analysis.MethodSummariser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.analysis.symbolic.SymbolicConstraint;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IntGraphTest {
  @Test
  public void addOverflowGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void greaterThanConstantRhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }
}
