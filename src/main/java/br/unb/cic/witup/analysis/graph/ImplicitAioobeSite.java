package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;

// Identifies a Jimple array element access (`arr[i]`, read or write) for the purpose of
// synthesising an ArrayIndexOutOfBoundsException predicate. Carries both the array base
// and the index Immediate; the bounds-check predicate built from these is
// `i < 0 || i >= arr.length`, conjoined with `arr != null` so the AIOOBE witness doesn't
// conflate with the NPE-on-array-base case (the JVM raises NPE before AIOOBE).
public record ImplicitAioobeSite(WITUpNode node, Local arrayBase, Immediate index) {}
