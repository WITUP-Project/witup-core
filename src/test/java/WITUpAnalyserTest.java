import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.SymExpr;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
import br.unb.cic.witup.graph.node.WITUpNode;
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
import java.util.List;

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

        WITUpGraph witUpCPG = WITUpGraph.fromPropertyGraph(sootUpCPG);

        List<WITUpNode> throwNodes = WITUpGraph.findThrowNodes(witUpCPG);
        assertEquals(1, throwNodes.size());

        List<WITUpNode> conditionNodes = WITUpGraph.findConditionNodes(witUpCPG, (ThrowStatementNode) throwNodes.get(0));
        assertEquals(1, conditionNodes.size());

        PropertyGraph sootUpCFG = circleAreaGraphs.getCFG();
        WITUpGraph witUpCFG = WITUpGraph.fromPropertyGraph(sootUpCFG);

        List<List<ThrowCondition>> throwConditionsPaths = WITUpGraph.findConditionPaths(witUpCFG, throwNodes.get(0));

        PropertyGraph sootUpDDG = circleAreaGraphs.getDDG();
        WITUpGraph witUpDDG = WITUpGraph.fromPropertyGraph(sootUpDDG);

        List<List<ResolvedThrowCondition>> resolvedConditionPaths = Resolver.resolveConditionPaths(throwConditionsPaths, witUpDDG);

//        for each path (List<ThrowCondition>) need to resolve the nodes and
//        return a (List<ResolvedThrowCondition>)
//        SymExpr resolved = Resolver.resolveThrowCondition(conditionNodes.get(0), witUpDDG);

        String dotGraph = sootUpDDG.toDotGraph();

        try {
            Graphviz.fromString(dotGraph)
                    .render(Format.SVG)
                    .toFile(new File("circleAreaGraph-ddg.svg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
