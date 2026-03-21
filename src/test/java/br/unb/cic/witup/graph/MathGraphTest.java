package br.unb.cic.witup.graph;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MathGraphTest {
  @Test
  public void invalidFieldGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: double circleArea()>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void invalidParameterGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int invalidParameter(int,int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void invalidParameterConjunction() {
    String methodSignature =
            "<br.unb.cic.witup.samples.Math: int invalidParameterConjunction(int)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(2, conditionNodes.size());
  }

  @Test
  public void truncateGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncate(double)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void truncateInlineGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Math: int truncateInline(double)>";

    WITUpGraph cpg = TestAnalysisContext.getGraphs().get(methodSignature);
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
            cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }
}
