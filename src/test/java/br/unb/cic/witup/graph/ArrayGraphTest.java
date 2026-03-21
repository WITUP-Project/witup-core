package br.unb.cic.witup.graph;

import br.unb.cic.witup.analysis.MethodSummariser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
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

  @Test
  public void getStringElementGraph() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: java.lang.String getStringElement(java.lang.String[],int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void getObjectElementGraph() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: java.lang.Object getObjectElement(java.lang.Object[],int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void getObjectFromArrayGraph() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: br.unb.cic.witup.samples.Array$MyObject getObjectFromArray(br.unb.cic.witup.samples.Array$MyObject[],int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void sumUntilZeroGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZero(int[])>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());

  }

  @Test
  public void sumUntilZeroWhileGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroWhile(int[])>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<List<SymbolicConstraint>> paths =
            new MethodSummariser(cpg).buildSymbolicConstraintPaths(throwNodes.get(0));
    assertEquals(1, paths.size());
  }

  @Test
  public void sumUntilZeroForEachGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int sumUntilZeroForEach(int[])>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());
  }

  @Test
  public void requireNonNullObjectGraph() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Array: void requireNonNullObject(java.lang.Object)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void requireNonNullArrayGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Array: int[] requireNonNullArray(int[])>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }
}
