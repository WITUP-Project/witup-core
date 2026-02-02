import static org.junit.jupiter.api.Assertions.*;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.expath.ExpathCache;
import br.unb.cic.witup.analysis.expath.ExpathOptions;
import br.unb.cic.witup.analysis.expath.GlobalExpathBuilder;
import br.unb.cic.witup.analysis.expath.InMemoryExpathCache;
import br.unb.cic.witup.analysis.expath.LocalExpathBuilder;
import br.unb.cic.witup.analysis.expath.ParameterBinder;
import br.unb.cic.witup.analysis.expath.SootUpCallSiteFinder;
import br.unb.cic.witup.helper.ExpathPrettyPrinter;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GlobalExpathBuilderTest {

    private Path testClassesDir;
    private SootUpAnalyser analyser;

    private Map<String, SootUpPropertyGraphs> graphs;

    private LocalExpathBuilder localBuilder;
    private GlobalExpathBuilder globalBuilder;

    @BeforeAll
    void init() {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        testClassesDir = projectRoot.resolve("target/test-classes");
        analyser = new SootUpAnalyser();

        graphs = new HashMap<>();

        // Math (optional)
        graphs.putAll(analyser.analyseThrowingMethods(testClassesDir.toString(), "br.unb.cic.witup.samples.Math"));

        // Simple class for the global expath concept
        graphs.putAll(
                analyser.analyseThrowingMethods(testClassesDir.toString(), "br.unb.cic.witup.samples.SafeDivision"));

        assertNotNull(graphs);
        assertFalse(graphs.isEmpty(), "No graphs produced by analyser.");

        ExpathCache cache = new InMemoryExpathCache();

        localBuilder = new LocalExpathBuilder(ExpathOptions.DEFAULT);
        globalBuilder =
                new GlobalExpathBuilder(
                        localBuilder,
                        new SootUpCallSiteFinder(graphs.keySet()),
                        new ParameterBinder(cache),
                        cache,
                        ExpathOptions.DEFAULT);
    }

    // ------------------------------------------------------------
    // SAFE DIVISION (global expath concept)
    // ------------------------------------------------------------

    @Test
    void safeDivision_divideWithValidatedOperands_generatesExpectedGlobalExpaths_andIsStableWithCache() {
        String root = findSignatureOrFail("divideWithValidatedOperands");

        // Pretty print local expaths (RAW CFG guards)
        ExpathPrettyPrinter.printLocalExpathsRawCfgGuards(root, graphs.get(root));

        // Local/global (first run)
        List<List<ResolvedThrowCondition>> local1 = buildLocal(root);
        List<List<ResolvedThrowCondition>> global1 = buildGlobal(root);

        ExpathPrettyPrinter.printResolvedPaths("LOCAL_RESOLVED", root, local1);
        ExpathPrettyPrinter.printResolvedPaths("GLOBAL_RESOLVED", root, global1);

        assertEquals(5, global1.size());
        assertTrue(global1.size() >= local1.size());
        assertTrue(global1.size() > local1.size());

        // Second run (should hit cache, but results must be identical in size/shape)
        List<List<ResolvedThrowCondition>> global2 = buildGlobal(root);
        assertEquals(global1.size(), global2.size(), "Global expath count changed between cached runs.");
    }

    // ------------------------------------------------------------
    // MATH (optional)
    // ------------------------------------------------------------

    @Test
    void math_calculateLogInBase_generatesExpectedGlobalExpaths() {
        String root = findSignatureOrFail("calculateLogInBase");

        ExpathPrettyPrinter.printLocalExpathsRawCfgGuards(root, graphs.get(root));

        List<List<ResolvedThrowCondition>> local = buildLocal(root);
        List<List<ResolvedThrowCondition>> global = buildGlobal(root);

        ExpathPrettyPrinter.printResolvedPaths("LOCAL_RESOLVED", root, local);
        ExpathPrettyPrinter.printResolvedPaths("GLOBAL_RESOLVED", root, global);

        assertEquals(5, global.size());
        assertTrue(global.size() > local.size());
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private List<List<ResolvedThrowCondition>> buildLocal(String methodSignature) {
        SootUpPropertyGraphs g = graphs.get(methodSignature);
        assertNotNull(g, "Graphs not found for " + methodSignature);
        return localBuilder.buildLocalResolvedPaths(g);
    }

    private List<List<ResolvedThrowCondition>> buildGlobal(String rootSignature) {
        return globalBuilder.buildGlobalResolvedPaths(new HashMap<>(graphs), rootSignature);
    }

    private String findSignatureOrFail(String methodName) {
        return graphs.keySet().stream()
                .filter(sig -> sig.contains(methodName))
                .findFirst()
                .orElseThrow(
                        () ->
                                new AssertionError(
                                        "Could not find method signature containing '"
                                                + methodName
                                                + "'. Available:\n"
                                                + String.join("\n", graphs.keySet())));
    }
}
