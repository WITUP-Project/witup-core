package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.constant.MethodHandle;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;
import sootup.core.jimple.common.stmt.JAssignStmt;

public class IntGraphTest {
  @Test
  public void addOverflowGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
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

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
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

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void equalsConstantLhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int equalsConstantLhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
    assertNotNull(cpg);

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void negatedLessThanConstantRhsGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int negatedLessThanConstantRhs(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
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

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();
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

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

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
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void applyAndCheckGraph() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Int: java.lang.Integer lambda$applyAndCheck$0(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());
  }

  @Test
  public void applyAndCheckResultGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int applyAndCheckResult(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());

    List<WITUpNode> conditionNodes =
        cpg.getThrowConditionNodes((ThrowStatementNode) throwNodes.get(0));
    assertEquals(1, conditionNodes.size());

    cpg.vertexSet()
        .forEach(
            node -> {
              if (node instanceof SimpleNode sn
                  && sn.getNode() instanceof StmtGraphNode stmtNode
                  && stmtNode.getStmt() instanceof JAssignStmt assign
                  && assign.getRightOp() instanceof JDynamicInvokeExpr invoke) {
                System.out.println("DynamicInvoke: " + invoke);
                invoke
                    .getBootstrapArgs()
                    .forEach(
                        arg ->
                            System.out.println(
                                "  bootstrap arg: " + arg.getClass().getName() + " = " + arg));
                invoke
                    .getArgs()
                    .forEach(
                        arg ->
                            System.out.println("  arg: " + arg.getClass().getName() + " = " + arg));
                invoke
                    .getBootstrapArgs()
                    .forEach(
                        arg -> {
                          if (arg instanceof MethodHandle mh) {
                            System.out.println("MethodHandle methods:");
                            System.out.println("  toString: " + mh);
                            System.out.println(
                                "  getReferenceSignature: "
                                    + mh.getReferenceSignature().getSubSignature());
                          }
                        });
              }
            });
  }
}
