package br.unb.cic.witup.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.unb.cic.witup.analysis.AnalysisResult;
import br.unb.cic.witup.analysis.graph.ImplicitAioobeSite;
import br.unb.cic.witup.analysis.graph.ImplicitArithmeticSite;
import br.unb.cic.witup.analysis.graph.ImplicitNegativeArraySizeSite;
import br.unb.cic.witup.analysis.graph.ImplicitNpeReceiverSite;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ImplicitsGraphTest {
  @Test
  public void receiverNpeGraph() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int receiverNpe(java.lang.Object)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<ImplicitNpeReceiverSite> sites = cpg.getImplicitNpeReceiverSites();
    // receiverNpe contains exactly one instance invoke (`o.hashCode()`).
    assertEquals(1, sites.size());
  }

  @Test
  public void fieldNpeGraph() {
    String methodSignature =
        "<br.unb.cic.witup.samples.Implicits: int fieldNpe(br.unb.cic.witup.samples.Implicits$Box)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<ImplicitNpeReceiverSite> sites = cpg.getImplicitNpeReceiverSites();
    // fieldNpe contains exactly one instance field read (`b.x`).
    assertEquals(1, sites.size());
  }

  @Test
  public void arrayDerefGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int arrayDeref(int[])>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    // arrayDeref contains exactly one array element access (`arr[0]`); the array base is
    // an NPE-receiver site, and the same access is an AIOOBE site for the index.
    List<ImplicitNpeReceiverSite> npeSites = cpg.getImplicitNpeReceiverSites();
    assertEquals(1, npeSites.size());
    List<ImplicitAioobeSite> aioobeSites = cpg.getImplicitAioobeSites();
    assertEquals(1, aioobeSites.size());
  }

  @Test
  public void negativeArraySizeGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int[] negativeArraySize(int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<ImplicitNegativeArraySizeSite> sites = cpg.getImplicitNegativeArraySizeSites();
    // negativeArraySize contains exactly one `new int[n]` allocation.
    assertEquals(1, sites.size());
  }

  @Test
  public void divByZeroGraph() {
    String methodSignature = "<br.unb.cic.witup.samples.Implicits: int divByZero(int,int)>";
    AnalysisResult analysis = TestAnalysisContext.getAnalyser().analyseMethod(methodSignature);
    WITUpGraph cpg = analysis.graph();

    List<ImplicitArithmeticSite> sites = cpg.getImplicitArithmeticSites();
    // divByZero contains exactly one integer division (`a / b`).
    assertEquals(1, sites.size());
  }
}
