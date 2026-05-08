package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FileGraphTest {
  @Test
  public void verifiedListFilesGraph() {
    String methodSignature =
        "<br.unb.cic.witup.samples.File: java.io.File[] verifiedListFiles(java.io.File)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(3, throwNodes.size());
  }

  @Test
  public void cleanDirectoryGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.File: void cleanDirectory(java.io.File)>";

    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<WITUpNode> throwNodes = cpg.getThrowNodes();
    assertEquals(1, throwNodes.size());
  }
}
