package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.graph.MethodCallSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CallsGraphTest {
  @Test
  public void unguardedCalleeThrowGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Calls: void unguardedCalleeThrow(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    // Exactly one call site: `calleeThrows(x)`. Static method, so actuals is just the
    // single int parameter — no receiver appended.
    List<MethodCallSite> sites = cpg.getCallSites();
    assertEquals(1, sites.size());
    MethodCallSite site = sites.get(0);
    assertEquals(
        "<br.unb.cic.witup.samples.Calls: void calleeThrows(int)>", site.calleeSignature());
    assertEquals(1, site.actuals().size());
  }
}
