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

public class ArrayGraphTest {
  @Test
  public void getElementGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int getElement(int[],int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void checkLengthGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int checkLength(int[])>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void allocateGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] allocate(int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }
}
