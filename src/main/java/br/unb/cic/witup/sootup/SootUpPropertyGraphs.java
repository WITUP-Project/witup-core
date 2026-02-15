package br.unb.cic.witup.sootup;

import sootup.codepropertygraph.propertygraph.PropertyGraph;

public final class SootUpPropertyGraphs {
  private String methodSignature;
  private PropertyGraph cfg;
  private PropertyGraph cdg;
  private PropertyGraph ddg;
  private PropertyGraph cpg;

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

  public PropertyGraph getCFG() {
    return cfg;
  }

  public PropertyGraph getCDG() {
    return cdg;
  }

  public PropertyGraph getCPG() {
    return cpg;
  }

  public PropertyGraph getDDG() {
    return ddg;
  }

  public String getMethodSignature() {
    return methodSignature;
  }
}
