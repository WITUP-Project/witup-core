package br.unb.cic.witup.analysis.expath;

import br.unb.cic.witup.analysis.expath.records.CallSite;
import br.unb.cic.witup.graph.WITUpGraph;
import java.util.List;

public interface CallSiteFinder {
    List<CallSite> findCallSites(WITUpGraph callerCfg);
}
