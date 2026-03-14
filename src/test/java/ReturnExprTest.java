import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.analysis.ClassAnalyser;
import br.unb.cic.witup.analysis.MethodSummariser;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ProjectAnalyser;
import br.unb.cic.witup.analysis.graph.WITUpGraph;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReturnExprTest {
  Map<String, WITUpGraph> witupGraphs;

  @BeforeAll
  void setUp() {
    Path testClassesDir = Paths.get(System.getProperty("user.dir")).resolve("target/test-classes");
    witupGraphs =
        ProjectAnalyser.buildGraphsForClass(
            new ClassAnalyser(testClassesDir.toString(), "br.unb.cic.witup.samples.Int").load());
  }

  @Test
  public void addReturnExpr() {
    String methodSignature = "<br.unb.cic.witup.samples.Int: int add(int,int)>";
    WITUpGraph cpg = witupGraphs.get(methodSignature);
    MethodSummariser analysis = new MethodSummariser(cpg);
    MethodSummary summary = analysis.summarise();

    assertNotNull(summary.getReturnExpr());
    assertNotNull(summary.getFormalParams());
    assertEquals(2, summary.getFormalParams().size());
    System.out.println("returnExpr: " + summary.getReturnExpr());
    System.out.println("formals: " + summary.getFormalParams());
  }
}
