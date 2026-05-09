package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.core.jimple.basic.Immediate;

// Identifies a Jimple integer division or remainder (`a / b`, `a % b`). If the divisor is
// zero at runtime, the JVM raises ArithmeticException — no `athrow` in the bytecode.
// Floating-point division by zero produces ±Infinity or NaN rather than an exception, so
// only integer-typed (`IntType`, `LongType`) operations are emitted as sites here.
public record ImplicitArithmeticSite(WITUpNode node, Immediate divisor) {}
