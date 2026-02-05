package br.unb.cic.witup.analysis.expath.cache;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.sootup.SootUpPropertyGraphs;

import java.util.List;
import java.util.Map;

public interface GlobalExpathPort {
    List<List<ResolvedThrowCondition>> compose(
            Map<String, SootUpPropertyGraphs> graphsBySignature,
            String rootSignature,
            int maxDepth);
}
