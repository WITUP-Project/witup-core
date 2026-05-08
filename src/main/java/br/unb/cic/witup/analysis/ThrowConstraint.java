package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.node.WITUpNode;

// forwardEdgeIndex is the position of the originating CFG edge in the path's forward
// edge sequence (path.edges() reversed). Two BooleanCFGEdges into the same loop-header
// node — e.g. enter-vs-exit on either side of a back-edge crossing — share `node` but
// differ in forwardEdgeIndex, which is what lets the substituter pick the correct
// loop-iteration symbol when rewriting the constraint.
public record ThrowConstraint(WITUpNode node, boolean truthValue, int forwardEdgeIndex) {}
