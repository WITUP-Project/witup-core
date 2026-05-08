package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CatchesGraphTest {
  @Test
  public void simpleCatchGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Catches: void simpleCatch(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    // simpleCatch contains exactly one throw stmt (`throw new IllegalArgumentException`).
    assertEquals(1, throwNodes.size());
  }
}
