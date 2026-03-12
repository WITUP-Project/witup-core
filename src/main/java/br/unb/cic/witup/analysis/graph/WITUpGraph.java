package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.CFGEdge;
import br.unb.cic.witup.analysis.graph.edge.ControlDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.GotoCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.IfStatementNode;
import br.unb.cic.witup.analysis.graph.node.ReturnStatementNode;
import br.unb.cic.witup.analysis.graph.node.SimpleNode;
import br.unb.cic.witup.analysis.graph.node.ThrowStatementNode;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import sootup.codepropertygraph.propertygraph.PropertyGraph;
import sootup.codepropertygraph.propertygraph.edges.CdgEdge;
import sootup.codepropertygraph.propertygraph.edges.DdgEdge;
import sootup.codepropertygraph.propertygraph.edges.GotoCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.IfFalseCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.IfTrueCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.NormalCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.java.core.JavaSootMethod;

/** A graph representation for control property graphs extending JGraphT's DirectedPseudograph. */
public final class WITUpGraph extends DirectedPseudograph<WITUpNode, WITUpEdge> {
  private String methodSignature;
  private JavaSootMethod method;

  public String getMethodSignature() {
    return methodSignature;
  }

  public JavaSootMethod getMethod() {
    return method;
  }

  private WITUpGraph() {
    super(WITUpEdge.class);
  }

  /**
   * Creates a WITUpGraph from PropertyGraph from SootUp
   *
   * @param pg the PropertyGraph to convert
   * @return the converted WITUpGraph
   */
  public static WITUpGraph fromPropertyGraph(final PropertyGraph pg, final JavaSootMethod method) {
    WITUpGraph graph = new WITUpGraph();
    Map<PropertyGraphNode, WITUpNode> cachedNodes = new HashMap<>();
    graph.methodSignature = method.getSignature().toString();
    graph.method = method;

    for (PropertyGraphEdge edge : pg.getEdges()) {
      WITUpNode source = cachedNodes.computeIfAbsent(edge.getSource(), WITUpGraph::createNode);
      WITUpNode target = cachedNodes.computeIfAbsent(edge.getDestination(), WITUpGraph::createNode);

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
      } else if (edge instanceof GotoCfgEdge) {
        graph.addEdge(source, target, new GotoCFGEdge(edge, source, target));
      } else {
        throw new IllegalArgumentException("bad edge type: " + edge.getClass().getName());
      }
    }

    return graph;
  }

  private static WITUpNode createNode(final PropertyGraphNode node) {
    if (node instanceof StmtGraphNode stmt && stmt.getStmt() instanceof JThrowStmt throwStmt) {
      return new ThrowStatementNode(node, throwStmt.getOp());
    } else if (node instanceof StmtGraphNode stmt && stmt.getStmt() instanceof JIfStmt ifStmt) {
      return new IfStatementNode(node, ifStmt.getCondition());
    } else if (node instanceof StmtGraphNode stmt
        && stmt.getStmt() instanceof JReturnStmt returnStmt) {
      return new ReturnStatementNode(node, returnStmt);
    }
    return new SimpleNode(node);
  }

  public List<WITUpNode> getThrowNodes() {
    List<WITUpNode> result = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (n instanceof ThrowStatementNode) {
        result.add(n);
      }
    }
    return result;
  }

  public List<WITUpNode> getThrowConditionNodes(final ThrowStatementNode t) {
    List<WITUpNode> throwConditionNodes = new ArrayList<>();
    EdgeReversedGraph<WITUpNode, WITUpEdge> reversedGraph = new EdgeReversedGraph<>(this);
    Iterator<WITUpNode> iterator = new DepthFirstIterator<>(reversedGraph, t);
    while (iterator.hasNext()) {
      WITUpNode n = iterator.next();
      if (n instanceof IfStatementNode) {
        throwConditionNodes.add(n);
      }
    }

    return throwConditionNodes;
  }

  public List<GraphPath<WITUpNode, WITUpEdge>> getConstraintPaths(final WITUpNode throwNode) {
    WITUpNode entry = findEntryNode();

    // Getting the CFG here removes all redundancy (hundreds less paths)
    AsSubgraph<WITUpNode, WITUpEdge> cfg = getCfg();

    AllDirectedPaths<WITUpNode, WITUpEdge> allPaths = new AllDirectedPaths<>(cfg);
    List<GraphPath<WITUpNode, WITUpEdge>> throwPaths =
        allPaths.getAllPaths(entry, throwNode, true, null);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithIfStatements = new ArrayList<>();
    for (GraphPath<WITUpNode, WITUpEdge> path : throwPaths) {
      for (WITUpNode node : path.getVertexList()) {
        if (node instanceof IfStatementNode) {
          pathsWithIfStatements.add(path);
          break;
        }
      }
    }
    return pathsWithIfStatements;
  }

  private AsSubgraph<WITUpNode, WITUpEdge> getCfg() {
    return new AsSubgraph<>(
        this,
        null,
        this.edgeSet().stream()
            .filter(edge -> edge instanceof CFGEdge)
            .collect(Collectors.toSet()));
  }

  private WITUpNode findEntryNode() {
    Set<PropertyGraphNode> hasIncoming = new HashSet<>(this.vertexSet().size());

    for (WITUpEdge e : this.edgeSet()) {
      hasIncoming.add(e.getEdge().getDestination());
    }

    for (WITUpNode witNode : this.vertexSet()) {
      PropertyGraphNode pgNode = witNode.getNode();

      if (hasIncoming.contains(pgNode)) {
        continue;
      }

      if (pgNode instanceof StmtGraphNode stmtNode && stmtNode.getStmt() instanceof JIdentityStmt) {
        return witNode;
      }
    }

    throw new IllegalStateException("No entry JIdentityStmt node in graph");
  }

  public List<ThrowConstraint> getThrowConstraints(final GraphPath<WITUpNode, WITUpEdge> path) {
    List<ThrowConstraint> throwConstraints = new ArrayList<>();
    for (WITUpEdge e : path.getEdgeList()) {
      if (e instanceof BooleanCFGEdge) {
        WITUpNode sourceNode = e.getSource();
        throwConstraints.add(new ThrowConstraint(sourceNode, ((BooleanCFGEdge) e).getCondition()));
      }
    }
    return throwConstraints;
  }

  public List<DataDependencyEdge> getIncomingDDGEdges(final WITUpNode node) {
    List<DataDependencyEdge> edges = new ArrayList<>();
    for (WITUpEdge edge : this.incomingEdgesOf(node)) {
      if (edge instanceof DataDependencyEdge) {
        edges.add((DataDependencyEdge) edge);
      }
    }
    return edges;
  }
}
