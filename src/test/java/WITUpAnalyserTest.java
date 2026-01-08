import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// For now this is our basic test runner that will do an e2e run of sorts. We
// will need to break this up soon
public class WITUpAnalyserTest {
    Path projectRoot = Paths.get(System.getProperty("user.dir"));
    Path testClassesDir = projectRoot.resolve("target/test-classes");

    @Test
    public void buildSootUpGraphBundleForThrowingMethods() {
        SootUpAnalyser analyser = new SootUpAnalyser();
        HashMap<String, SootUpPropertyGraphs> sootupGraphs = analyser
                .analyseThrowingMethods(testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

        assertNotNull(sootupGraphs);
        assertEquals(3, sootupGraphs.size());
    }

}
