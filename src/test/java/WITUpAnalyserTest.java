import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.sootup.SootUpAnalyser;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import sootup.codepropertygraph.propertygraph.PropertyGraph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// For now this is our basic test runner that will do an e2e run of sorts. We
// will need to break this up soon
public class WITUpAnalyserTest {
    private Path testClassesDir;
    private SootUpAnalyser sootUpAnalyser;

    @BeforeEach
    void setUp() {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        testClassesDir = projectRoot.resolve("target/test-classes");
        sootUpAnalyser = new SootUpAnalyser();
    }


    @Test
    public void buildSootUpPropertyGraphs() {
        HashMap<String, SootUpPropertyGraphs> sootupGraphs = sootUpAnalyser
                .analyseThrowingMethods(testClassesDir.toString(), "br.unb.cic.witup.samples.Math");

        assertNotNull(sootupGraphs);
        assertEquals(3, sootupGraphs.size());
    }

    @Test
    public void invalidField() {
        HashMap<String, SootUpPropertyGraphs> sootUpPropertyGraphs = sootUpAnalyser
                .analyseThrowingMethods(testClassesDir.toString(), "br.unb.cic.witup.samples.Math");


        SootUpPropertyGraphs circleAreaGraphs = sootUpPropertyGraphs.get("<br.unb.cic.witup.samples.Math: double circleArea()>");
        PropertyGraph sootUpCPG = circleAreaGraphs.getCPG();

        System.out.println(sootUpCPG);

        String dotGraph = sootUpCPG.toDotGraph();

        try {
            Graphviz.fromString(dotGraph)
                    .render(Format.SVG)
                    .toFile(new File("circleAreaGraph.svg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);
    }

}
