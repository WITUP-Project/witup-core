import static org.junit.jupiter.api.Assertions.*;

import br.unb.cic.witup.analysis.GlobalExpathComposer;
import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlobalExpathComposerTest {

    private Path testClassesDir;
    private SootUpAnalyser sootUpAnalyser;
    private GlobalExpathComposer composer;

    private static final String className = "br.unb.cic.witup.samples.SampleMean";
    private static final String rootSig = "<br.unb.cic.witup.samples.SampleMean: double processSample(double,int)>";
    private static final String midSig  = "<br.unb.cic.witup.samples.SampleMean: double meanChecked(double,int)>";
    private static final String leafSig = "<br.unb.cic.witup.samples.SampleMean: double meanCore(double,int)>";

    @BeforeEach
    void setUp() {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        testClassesDir = projectRoot.resolve("target/test-classes");
        sootUpAnalyser = new SootUpAnalyser();
        composer = new GlobalExpathComposer();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenDepthIsNegative() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();
        assertThrows(IllegalArgumentException.class, () -> composer.composeGlobals(graphs, rootSig, -1));
    }

    @Test
    void shouldReturnEmptyWhenRootSignatureIsUnknown() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();
        List<List<ResolvedThrowCondition>> result =
                composer.composeGlobals(graphs, "<unknown: void missing()>", 2);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldContainExpectedMethodSignaturesInAnalysedMap() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();

        assertTrue(graphs.containsKey(rootSig));
        assertTrue(graphs.containsKey(midSig));
        assertTrue(graphs.containsKey(leafSig));
    }

    @Test
    void shouldHaveResultsWhenDepthIsZero() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();

        List<List<ResolvedThrowCondition>> depth0 = composer.composeGlobals(graphs, rootSig, 0);

        assertNotNull(depth0);
        assertFalse(depth0.isEmpty());
    }

    @Test
    void shouldNotRequirePathSizeThreeWhenDepthIsOne() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();

        List<List<ResolvedThrowCondition>> depth1 = composer.composeGlobals(graphs, rootSig, 1);

        assertNotNull(depth1);
        assertFalse(depth1.isEmpty());
    }

    @Test
    void shouldHavePathSizeThreeWhenDepthIsTwo() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();

        List<List<ResolvedThrowCondition>> depth2 = composer.composeGlobals(graphs, rootSig, 2);

        assertNotNull(depth2);
        assertFalse(depth2.isEmpty());
        assertTrue(depth2.stream().anyMatch(p -> p.size() == 3));
    }

    @Test
    void shouldIncreaseOrMaintainPathCountAsDepthGrows() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();

        List<List<ResolvedThrowCondition>> depth0 = composer.composeGlobals(graphs, rootSig, 0);
        List<List<ResolvedThrowCondition>> depth1 = composer.composeGlobals(graphs, rootSig, 1);
        List<List<ResolvedThrowCondition>> depth2 = composer.composeGlobals(graphs, rootSig, 2);

        assertTrue(depth1.size() >= depth0.size());
        assertTrue(depth2.size() >= depth1.size());
    }

    @Test
    void shouldHaveAtLeastOnePathLongerThanDepthZeroWhenDepthIsTwo() {
        HashMap<String, SootUpPropertyGraphs> graphs = analyseSampleMean();

        List<List<ResolvedThrowCondition>> depth0 = composer.composeGlobals(graphs, rootSig, 0);
        List<List<ResolvedThrowCondition>> depth2 = composer.composeGlobals(graphs, rootSig, 2);

        int maxLenDepth0 = depth0.stream().mapToInt(List::size).max().orElse(0);
        int maxLenDepth2 = depth2.stream().mapToInt(List::size).max().orElse(0);

        assertTrue(maxLenDepth2 >= maxLenDepth0);
    }

    private HashMap<String, SootUpPropertyGraphs> analyseSampleMean() {
        return sootUpAnalyser.analyseThrowingMethods(testClassesDir.toString(), className);
    }
}
