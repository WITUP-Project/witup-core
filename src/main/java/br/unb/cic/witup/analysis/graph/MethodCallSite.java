package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.List;
import sootup.core.jimple.basic.Immediate;

// Identifies a Jimple call site: the WITUpNode wrapping the call statement, the callee's
// soot-format signature, and the actual arguments. Actuals are ordered to match
// MethodSummariser.buildFormals(): `[arg0, arg1, ..., argN]` for static calls, with the
// receiver appended at the end (`@this` index -1) for instance calls. Consumed by the
// rollup phase to compose callee escape sets into the caller's summary.
public record MethodCallSite(WITUpNode node, String calleeSignature, List<Immediate> actuals) {}
