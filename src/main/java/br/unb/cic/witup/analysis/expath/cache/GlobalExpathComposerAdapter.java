package br.unb.cic.witup.analysis.expath.cache;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.expath.GlobalExpathComposer;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;

import java.util.List;
import java.util.Map;

public final class GlobalExpathComposerAdapter implements GlobalExpathPort {
    private final GlobalExpathComposer composer = new GlobalExpathComposer();

    @Override
    public List<List<ResolvedThrowCondition>> compose(
            Map<String, SootUpPropertyGraphs> graphsBySignature,
            String rootSignature,
            int maxDepth) {
        return composer.composeGlobals(graphsBySignature, rootSignature, maxDepth);
    }

}

