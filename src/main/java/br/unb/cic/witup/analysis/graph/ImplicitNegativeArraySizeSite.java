package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.core.jimple.basic.Immediate;

// Identifies a Jimple `new T[n]` allocation. If the size operand is negative at runtime,
// the JVM raises NegativeArraySizeException — no `athrow` in the bytecode. Carries the
// size Immediate so MethodSummariser can build the predicate `n < 0`. Single-dimensional
// arrays only; multi-dimensional (JNewMultiArrayExpr) is a follow-up.
public record ImplicitNegativeArraySizeSite(WITUpNode node, Immediate size) {}
