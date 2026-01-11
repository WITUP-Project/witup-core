package br.unb.cic.witup.sootup;

import sootup.codepropertygraph.propertygraph.PropertyGraph;

public class SootUpPropertyGraphs {
    String methodSignature;
    PropertyGraph cfg;
    PropertyGraph cdg;
    PropertyGraph ddg;
    PropertyGraph cpg;

    public SootUpPropertyGraphs(
            final String methodSignature,
            final PropertyGraph cfg,
            final PropertyGraph cdg,
            final PropertyGraph ddg,
            final PropertyGraph cpg) {

        this.methodSignature = methodSignature;
        this.cfg = cfg;
        this.cdg = cdg;
        this.ddg = ddg;
        this.cpg = cpg;
    }

    public PropertyGraph getCPG() {
        return cpg;
    }

}
