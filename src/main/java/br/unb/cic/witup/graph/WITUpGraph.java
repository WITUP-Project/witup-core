package br.unb.cic.witup.graph;

import br.unb.cic.witup.analysis.ThrowCondition;
import br.unb.cic.witup.graph.edge.*;
import br.unb.cic.witup.graph.node.IfStatementNode;
import br.unb.cic.witup.graph.node.SimpleNode;
import br.unb.cic.witup.graph.node.ThrowStatementNode;
import br.unb.cic.witup.graph.node.WITUpNode;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.edges.*;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;

import java.util.*;

/**
 * A graph representation for control property graphs extending JGraphT's DirectedPseudograph.
 */
public class WITUpGraph extends DirectedPseudograph<WITUpNode, WITUpEdge> {
    WITUpNode first;

    public WITUpNode getFirstNode() {
        return this.first;
    }

    private WITUpGraph() {
        super(WITUpEdge.class);
    }

    /**
     * Creates a WITUpGraph from <a href="https://soot-oss.github.io/SootUp/v2.0.0/codepropertygraphs/">SootUp's</a>
     * PropertyGraph type.
     *
     * @param pg the PropertyGraph to convert
     * @return the converted WITUpGraph
     */
    /*
    This couples WITUpGraph with SootUp. If we are ever going to process multiple languages, then
    we are going to need to decide whether to couple the Java frontend to SootUp or to add a
    serialisation layer before creating the WITUpGraph
     */
    public static WITUpGraph fromPropertyGraph(final PropertyGraph pg) {
        WITUpGraph graph = new WITUpGraph();

        for (PropertyGraphEdge edge : pg.getEdges()) {
            // TODO: remove once confirmed witup graphs are building properly without AST
            if (edge instanceof AbstAstEdge) {
                continue;
            }

            WITUpNode source = createNode(edge.getSource());
            WITUpNode target = createNode(edge.getDestination());
            graph.addVertex(source);
            graph.addVertex(target);

            if (edge instanceof DdgEdge) {
                graph.addEdge(source, target, new DataDependencyEdge(edge, source, target));
            } else if (edge instanceof CdgEdge) {
                graph.addEdge(source, target, new ControlDependencyEdge(edge, source, target));
            } else if (edge instanceof IfTrueCfgEdge) {
                graph.addEdge(source, target, new BooleanCFGEdge(edge, source, target, true));
            } else if (edge instanceof IfFalseCfgEdge) {
                graph.addEdge(source, target, new BooleanCFGEdge(edge, source, target, false));
            } else if (edge instanceof NormalCfgEdge) {
                graph.addEdge(source, target, new CFGEdge(edge, source, target));
            } else {
                throw new IllegalArgumentException("Unknown edge type: " + edge.getClass().getName());
            }
        }

        return graph;
    }

    private static WITUpNode createNode(final PropertyGraphNode node) {
        if (node instanceof StmtGraphNode stmt && stmt.getStmt() instanceof JThrowStmt throwStmt) {
            return new ThrowStatementNode(node, throwStmt.getOp());
        } else if (node instanceof StmtGraphNode stmt && stmt.getStmt() instanceof JIfStmt ifStmt) {
            return new IfStatementNode(node, ifStmt.getCondition());
        }
        return new SimpleNode(node);
    }

    public static List<WITUpNode> findThrowNodes(final WITUpGraph g) {
        List<WITUpNode> result = new ArrayList<>();
        for (WITUpNode n : g.vertexSet()) {
            if (n instanceof ThrowStatementNode) {
                result.add(n);
            }
        }
        return result;
    }

    public static List<WITUpNode> findConditionNodes(final WITUpGraph g, final ThrowStatementNode t) {
        List<WITUpNode> throwConditionNodes = new ArrayList<>();
        // Not sure how costly this reversal can be at scale. Doc says there is a penalty
        // We can build the reversed graph if we need
        EdgeReversedGraph<WITUpNode, WITUpEdge> reversedGraph = new EdgeReversedGraph<>(g);
        Iterator<WITUpNode> iterator = new DepthFirstIterator<>(reversedGraph, t);
        while (iterator.hasNext()) {
            WITUpNode n = iterator.next();
            if (n instanceof IfStatementNode) {
                throwConditionNodes.add(n);
            }
        }

        return throwConditionNodes;
    }


    public static WITUpNode findEntryNode(final WITUpGraph g) {
        // Track incoming edges by *PropertyGraphNode identity*
        Set<PropertyGraphNode> hasIncoming =
                new HashSet<>(g.vertexSet().size());

        for (WITUpEdge e : g.edgeSet()) {
            hasIncoming.add(e.getEdge().getDestination());
        }

        for (WITUpNode witNode : g.vertexSet()) {
            PropertyGraphNode pgNode = witNode.getNode();

            if (hasIncoming.contains(pgNode)) {
                continue;
            }

            if (pgNode instanceof StmtGraphNode stmtNode
                    && stmtNode.getStmt() instanceof JIdentityStmt) {
                return witNode;
            }
        }

        throw new IllegalStateException("No entry JIdentityStmt node in graph");
    }

    public static List<List<ThrowCondition>> findConditionPaths(WITUpGraph cfg, WITUpNode throwNode) {
        WITUpNode entry =  findEntryNode(cfg);

        AllDirectedPaths<WITUpNode, WITUpEdge> allPaths = new AllDirectedPaths<>(cfg);
        List<GraphPath<WITUpNode, WITUpEdge>> throwPaths = allPaths
                .getAllPaths(entry, throwNode, true, null);

        List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements = new ArrayList<>();
        for (GraphPath<WITUpNode, WITUpEdge> path : throwPaths) {
            for (WITUpNode node : path.getVertexList()) {
                if (node instanceof IfStatementNode) {
                    pathsWithIfStatements.add(path);
                    break;
                }
            }
        }

        List<List<BooleanCFGEdge>> throwConditionsPaths = new ArrayList<>();
        for (GraphPath<WITUpNode, WITUpEdge> path : pathsWithIfStatements) {
            List<BooleanCFGEdge> booleanEdges = new ArrayList<>();
            for (WITUpEdge edge : path.getEdgeList()) {
                if (edge instanceof BooleanCFGEdge) {
                    booleanEdges.add((BooleanCFGEdge) edge);
                    System.out.println(((BooleanCFGEdge) edge).getCondition());
                }
            }
            throwConditionsPaths.add(booleanEdges);
        }

        List<List<ThrowCondition>> throwConditions = new ArrayList<>();
        for (List<BooleanCFGEdge> throwConditionsPath : throwConditionsPaths) {
            List<ThrowCondition> pathConditions = new ArrayList<>();
            for (BooleanCFGEdge edge : throwConditionsPath) {
                WITUpNode sourceNode = edge.getSource();
                pathConditions.add(new ThrowCondition(sourceNode, edge.getCondition()));
            }
            throwConditions.add(pathConditions);
        }

        return throwConditions;
    }
}
