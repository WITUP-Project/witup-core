package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.graph.edge.BooleanCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.CFGEdge;
import br.unb.cic.witup.analysis.graph.edge.ControlDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.DataDependencyEdge;
import br.unb.cic.witup.analysis.graph.edge.ExceptionalCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.GotoCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.SwitchCFGEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.CaughtExceptionNode;
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
import sootup.codepropertygraph.propertygraph.edges.ExceptionalCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.GotoCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.IfFalseCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.IfTrueCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.NormalCfgEdge;
import sootup.codepropertygraph.propertygraph.edges.PropertyGraphEdge;
import sootup.codepropertygraph.propertygraph.edges.SwitchCfgEdge;
import sootup.codepropertygraph.propertygraph.nodes.PropertyGraphNode;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.java.core.JavaSootMethod;

/** A graph representation for control property graphs extending JGraphT's DirectedPseudograph. */
public final class WITUpGraph extends DirectedPseudograph<WITUpNode, WITUpEdge> {
  public static final int MAX_PATH_LENGTH = 50;
  private String methodSignature;
  private JavaSootMethod method;
  // dot for debugging purposes
  private String dot;
  private WITUpNode entryNode;
  private AllDirectedPaths<WITUpNode, WITUpEdge> allDirectedPaths;
  private final Map<WITUpNode, List<GraphPath<WITUpNode, WITUpEdge>>> cachedConstraintPaths =
      new HashMap<>();
  private final Map<WITUpNode, List<GraphPath<WITUpNode, WITUpEdge>>> cachedReturnPaths =
      new HashMap<>();

  public String getMethodSignature() {
    return methodSignature;
  }

  public JavaSootMethod getMethod() {
    return method;
  }

  private WITUpGraph() {
    super(WITUpEdge.class);
  }

  public String getDot() {
    return dot;
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
    graph.dot = pg.toDotGraph();

    for (PropertyGraphEdge edge : pg.getEdges()) {
      WITUpNode source = cachedNodes.computeIfAbsent(edge.getSource(), WITUpGraph::createNode);
      WITUpNode target = cachedNodes.computeIfAbsent(edge.getDestination(), WITUpGraph::createNode);

      graph.addVertex(source);
      graph.addVertex(target);

      if (edge instanceof DdgEdge) {
        graph.addEdge(source, target, new DataDependencyEdge(source, target));
      } else if (edge instanceof CdgEdge) {
        graph.addEdge(source, target, new ControlDependencyEdge(source, target));
      } else if (edge instanceof IfTrueCfgEdge) {
        graph.addEdge(source, target, new BooleanCFGEdge(source, target, true));
      } else if (edge instanceof IfFalseCfgEdge) {
        graph.addEdge(source, target, new BooleanCFGEdge(source, target, false));
      } else if (edge instanceof NormalCfgEdge) {
        graph.addEdge(source, target, new CFGEdge(source, target));
      } else if (edge instanceof GotoCfgEdge) {
        graph.addEdge(source, target, new GotoCFGEdge(source, target));
      } else if (edge instanceof ExceptionalCfgEdge) {
        graph.addEdge(source, target, new ExceptionalCFGEdge(source, target));
      } else if (edge instanceof SwitchCfgEdge) {
        graph.addEdge(source, target, new SwitchCFGEdge(source, target));
      } else {
        throw new IllegalArgumentException(
            "bad edge type: " + edge.getClass().getName() + "method: " + method.getSignature());
      }
    }

    return graph;
  }

  private static WITUpNode createNode(final PropertyGraphNode node) {
    if (node instanceof StmtGraphNode stmt && stmt.getStmt() instanceof JThrowStmt throwStmt) {
      return new ThrowStatementNode(node, throwStmt);
    } else if (node instanceof StmtGraphNode stmt && stmt.getStmt() instanceof JIfStmt ifStmt) {
      return new IfStatementNode(node, ifStmt.getCondition());
    } else if (node instanceof StmtGraphNode stmt
        && stmt.getStmt() instanceof JReturnStmt returnStmt) {
      return new ReturnStatementNode(node, returnStmt);
    } else if (node instanceof StmtGraphNode stmt
        && stmt.getStmt() instanceof JIdentityStmt identity
        && identity.getRightOp() instanceof JCaughtExceptionRef ref) {
      return new CaughtExceptionNode(node, ref);
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
    return cachedConstraintPaths.computeIfAbsent(throwNode, this::computeConstraintPaths);
  }

  private List<GraphPath<WITUpNode, WITUpEdge>> computeConstraintPaths(final WITUpNode throwNode) {
    WITUpNode entry = findEntryNode();
    List<GraphPath<WITUpNode, WITUpEdge>> throwPaths =
        getAllDirectedPaths().getAllPaths(entry, throwNode, true, null);

    List<GraphPath<WITUpNode, WITUpEdge>> pathsWithConstraints = new ArrayList<>();
    for (GraphPath<WITUpNode, WITUpEdge> path : throwPaths) {
      for (WITUpNode node : path.getVertexList()) {
        if (node instanceof IfStatementNode || node instanceof CaughtExceptionNode) {
          pathsWithConstraints.add(path);
          break;
        }
      }
    }
    return pathsWithConstraints;
  }

  private AllDirectedPaths<WITUpNode, WITUpEdge> getAllDirectedPaths() {
    if (allDirectedPaths == null) {
      allDirectedPaths = new AllDirectedPaths<>(getCfg());
    }
    return allDirectedPaths;
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
    if (entryNode != null) {
      return entryNode;
    }

    Set<WITUpNode> hasIncoming = new HashSet<>(this.vertexSet().size());

    for (WITUpEdge e : this.edgeSet()) {
      hasIncoming.add(e.getTarget());
    }

    for (WITUpNode witNode : this.vertexSet()) {
      if (hasIncoming.contains(witNode)) {
        continue;
      }

      PropertyGraphNode pgNode = witNode.getNode();
      if (pgNode instanceof StmtGraphNode stmtNode && stmtNode.getStmt() instanceof JIdentityStmt) {
        entryNode = witNode;
        return witNode;
      }
    }
    // lambdas and static initializers with no parameters
    for (WITUpNode witNode : this.vertexSet()) {
      if (!hasIncoming.contains(witNode)) {
        entryNode = witNode;
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
      } else if (e instanceof ExceptionalCFGEdge) {
        WITUpNode targetNode = e.getTarget();
        throwConstraints.add(new ThrowConstraint(targetNode, true));
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

  public List<ReturnStatementNode> getReturnNodes() {
    return vertexSet().stream()
        .filter(n -> n instanceof ReturnStatementNode)
        .map(n -> (ReturnStatementNode) n)
        .collect(Collectors.toList());
  }

  public List<GraphPath<WITUpNode, WITUpEdge>> getAllPathsToReturn(final WITUpNode returnNode) {
    return cachedReturnPaths.computeIfAbsent(returnNode, this::computeAllPathsToReturn);
  }

  private List<GraphPath<WITUpNode, WITUpEdge>> computeAllPathsToReturn(
      final WITUpNode returnNode) {
    WITUpNode entry = findEntryNode();
    return getAllDirectedPaths().getAllPaths(entry, returnNode, true, MAX_PATH_LENGTH);
  }

  // The Jimple pattern seems very consitent (hopefully an invariant):
  // $stack2 = new java.lang.IllegalArgumentException <--> first node has exception type
  // #l1 = (java.lang.Throwable) $stack 2             <--> mimic JVM wanting throwable
  // throw #l1                                        <--> actual throw node
  public String resolveExceptionType(final ThrowStatementNode throwNode) {
    for (DataDependencyEdge throwableEdge : getIncomingDDGEdges(throwNode)) {
      WITUpNode castNode = getEdgeSource(throwableEdge);
      for (DataDependencyEdge throwTypeEdge : getIncomingDDGEdges(castNode)) {
        WITUpNode newNode = getEdgeSource(throwTypeEdge);
        if (!(newNode instanceof SimpleNode simpleNode)) {
          continue;
        }
        if (!(simpleNode.getNode() instanceof StmtGraphNode stmtNode)) {
          continue;
        }
        if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
          continue;
        }
        if (assign.getRightOp() instanceof JNewExpr newExpr) {
          return newExpr.getType().getFullyQualifiedName();
        }
      }
    }
    return null;
  }

  public void dump() {
    for (WITUpNode n : vertexSet()) {
      System.out.println("  NODE: " + n.getClass().getSimpleName() + " -- " + n.getNode());
    }
    for (WITUpEdge e : edgeSet()) {
      System.out.println(
          "  EDGE: + "
              + e.getSource().getClass().getSimpleName()
              + "-->"
              + e.getTarget().getClass().getSimpleName());
    }
  }
}
