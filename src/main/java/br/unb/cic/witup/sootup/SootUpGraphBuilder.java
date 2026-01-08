package br.unb.cic.witup.sootup;

import sootup.codepropertygraph.cdg.CdgCreator;
import sootup.codepropertygraph.cfg.CfgCreator;
import sootup.codepropertygraph.ddg.DdgCreator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.java.core.JavaSootMethod;
import  sootup.codepropertygraph.propertygraph.util.PropertyGraphsMerger;

/**
 * Builds SootUp graphs that are required to build a Code Property Graph. It is
 * effectively the same implementation as SootUp's constructor for the CPG, but
 * we control the order of graph creation to avoid recomputing them. We also
 * discard the AST nodes at this stage. Later stages of the pipeline need
 * separate graphs during different parts of the analysis, so it's better to
 * construct them separately than to split them out later on.
 */
public final class SootUpGraphBuilder {
    public SootUpPropertyGraphs buildSootUpGraphBundle(final JavaSootMethod m) {
        CfgCreator cfgCreator = new CfgCreator();
        CdgCreator cdgCreator = new CdgCreator();
        DdgCreator ddgCreator = new DdgCreator();

        PropertyGraph cfg = cfgCreator.createGraph(m);
        PropertyGraph cdg = cdgCreator.createGraph(m);
        PropertyGraph ddg = ddgCreator.createGraph(m);

        PropertyGraph cpg = cfg;
        cpg = PropertyGraphsMerger.mergeGraphs(cpg, cdg);
        cpg = PropertyGraphsMerger.mergeGraphs(cpg, ddg);

        return new SootUpPropertyGraphs(
                m.getSignature().toString(),
                cfg,
                cdg,
                ddg,
                cpg
        );
    }
}
