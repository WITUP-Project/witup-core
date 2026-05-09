package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import sootup.core.jimple.basic.Local;

// Identifies a Jimple statement where a member access dereferences a Local — either an
// instance method invocation (`obj.method()`) or an instance field access (`obj.field`,
// read or write). The receiver being null at runtime triggers an implicit NPE — no
// `athrow` in the bytecode — which is what the rollup synthesises an ExceptionPath for.
public record ImplicitNpeReceiverSite(WITUpNode node, Local receiver) {}
