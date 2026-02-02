package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.Resolver;
import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.WITUpGraph;
import br.unb.cic.witup.graph.node.WITUpNode;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LocalExpathBuilder {

    private final ExpathOptions options;

    public LocalExpathBuilder() {
        this(ExpathOptions.DEFAULT);
    }

    public LocalExpathBuilder(final ExpathOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    public List<List<ResolvedThrowCondition>> buildLocalResolvedPaths(final SootUpPropertyGraphs graphs) {
        WITUpGraph cfg = WITUpGraph.fromPropertyGraph(graphs.getCFG());
        WITUpGraph ddg = WITUpGraph.fromPropertyGraph(graphs.getDDG());

        Resolver resolver = new Resolver(ddg);
        List<List<ResolvedThrowCondition>> out = new ArrayList<>();

        for (WITUpNode throwNode : WITUpGraph.findThrowNodes(cfg)) {
            List<List<ThrowCondition>> conditionPaths = WITUpGraph.findConditionPaths(cfg, throwNode, options);
            out.addAll(resolver.resolveConditionPaths(conditionPaths, ddg));
        }
        return out;
    }
}
