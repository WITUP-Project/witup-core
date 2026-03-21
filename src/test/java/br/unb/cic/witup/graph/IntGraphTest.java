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

public class IntGraphTest {
  @Test
  public void addOverflowGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void greaterThanConstantRhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int greaterThanConstantRhs(int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void lesserThanConstantLhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int lesserThanConstantLhs(int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void equalsConstantRhSGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantRhs(int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test public void equalsConstantLhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test public void negatedLessThanConstantRhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void lessThanConstantRhsViaBooleanGraph() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaBoolean(int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());
  }

  @Test
  public void lessThanConstantRhsViaNegatedBooleanGraph() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Int: int lessThanConstantRhsViaNegatedBoolean(int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());
  }

  @Test
  public void addAndCheckGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int addAndCheck(int,int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);

    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void negateValueGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negateValue(int)>";
    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }
}
