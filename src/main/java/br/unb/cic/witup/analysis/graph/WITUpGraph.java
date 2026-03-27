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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final int MAX_CONSTRAINT_PATHS = 8192;
  private String methodSignature;
  private JavaSootMethod method;
  // dot for debugging purposes
  private String dot;
  private WITUpNode entryNode;
  private final Map<WITUpNode, List<WITUpPath>> cachedConstraintPaths = new HashMap<>();
  private final Map<WITUpNode, List<WITUpPath>> cachedReturnPaths = new HashMap<>();
  private AsSubgraph<WITUpNode, WITUpEdge> cachedCfg;

  private static final Logger log = LoggerFactory.getLogger(WITUpGraph.class);

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

  public static int getMaxConstraintPaths() {
    return MAX_CONSTRAINT_PATHS;
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

  private record DFSEntry(WITUpNode node, WITUpEdge edge, DFSEntry parent) {
    boolean contains(final WITUpNode n) {
      for (DFSEntry e = this; e != null; e = e.parent) {
        if (e.node.equals(n)) {
          return true;
        }
      }
      return false;
    }

    WITUpPath toPath() {
      int len = 0;
      for (DFSEntry e = this; e != null; e = e.parent) {
        len++;
      }
      List<WITUpNode> nodes = new ArrayList<>(len);
      List<WITUpEdge> edges = new ArrayList<>(len - 1);
      for (DFSEntry e = this; e != null; e = e.parent) {
        nodes.add(e.node);
        if (e.edge != null) {
          edges.add(e.edge);
        }
      }
      return new WITUpPath(nodes, edges);
    }
  }

  private List<WITUpPath> backwardDFS(final WITUpNode start, final WITUpNode end) {
    List<WITUpPath> result = new ArrayList<>();
    Deque<DFSEntry> stack = new ArrayDeque<>();
    stack.push(new DFSEntry(end, null, null));

    while (!stack.isEmpty()) {
      DFSEntry current = stack.pop();

      if (current.node.equals(start)) {
        result.add(current.toPath());
        if (result.size() >= MAX_CONSTRAINT_PATHS) {
          log.info(
              "Path count truncated to {} for {} in {}",
              MAX_CONSTRAINT_PATHS,
              end,
              getMethodSignature());
          break;
        }
        continue;
      }

      for (WITUpEdge edge : getCfg().incomingEdgesOf(current.node)) {
        WITUpNode pred = edge.getSource();
        if (!current.contains(pred)) {
          stack.push(new DFSEntry(pred, edge, current));
        }
      }
    }
    return result;
  }

  private List<WITUpPath> forwardDFS(final WITUpNode start, final WITUpNode end) {
    List<WITUpPath> result = new ArrayList<>();
    Deque<WITUpPath> stack = new ArrayDeque<>();
    stack.push(new WITUpPath(new ArrayList<>(List.of(start)), new ArrayList<>()));

    while (!stack.isEmpty()) {
      WITUpPath current = stack.pop();
      WITUpNode tail = current.nodes().getLast();

      if (tail.equals(end)) {
        result.add(current);
        if (result.size() >= MAX_CONSTRAINT_PATHS) {
          log.info(
              "Return paths truncated to {} for {} in {}",
              MAX_CONSTRAINT_PATHS,
              end,
              getMethodSignature());
          break;
        }
        continue;
      }

      for (WITUpEdge edge : getCfg().outgoingEdgesOf(tail)) {
        WITUpNode succ = edge.getTarget();
        if (!current.nodes().contains(succ)) {
          List<WITUpNode> newNodes = new ArrayList<>(current.nodes());
          newNodes.add(succ);
          List<WITUpEdge> newEdges = new ArrayList<>(current.edges());
          newEdges.add(edge);
          stack.push(new WITUpPath(newNodes, newEdges));
        }
      }
    }
    return result;
  }

  public List<WITUpPath> getConstraintPaths(final WITUpNode throwNode) {
    return cachedConstraintPaths.computeIfAbsent(throwNode, this::computeConstraintPaths);
  }

  private List<WITUpPath> computeConstraintPaths(final WITUpNode throwNode) {
    WITUpNode entry = findEntryNode();
    List<WITUpPath> throwPaths = backwardDFS(entry, throwNode);
    List<WITUpPath> pathsWithConstraints = new ArrayList<>();
    for (WITUpPath path : throwPaths) {
      for (WITUpNode node : path.nodes()) {
        if (node instanceof IfStatementNode || node instanceof CaughtExceptionNode) {
          pathsWithConstraints.add(path);
          break;
        }
      }
      if (pathsWithConstraints.size() >= MAX_CONSTRAINT_PATHS) {
        break;
      }
    }
    return pathsWithConstraints;
  }

  private AsSubgraph<WITUpNode, WITUpEdge> getCfg() {
    if (cachedCfg != null) {
      return cachedCfg;
    }
    cachedCfg =
        new AsSubgraph<>(
            this,
            null,
            this.edgeSet().stream()
                .filter(edge -> edge instanceof CFGEdge)
                .collect(Collectors.toSet()));
    return cachedCfg;
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

  public List<ThrowConstraint> getThrowConstraints(final WITUpPath path) {
    List<ThrowConstraint> throwConstraints = new ArrayList<>();
    for (WITUpEdge e : path.edges()) {
      if (e instanceof BooleanCFGEdge boolEdge) {
        throwConstraints.add(new ThrowConstraint(e.getSource(), boolEdge.getCondition()));
      } else if (e instanceof ExceptionalCFGEdge) {
        throwConstraints.add(new ThrowConstraint(e.getTarget(), true));
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

  public List<WITUpPath> getAllPathsToReturn(final WITUpNode returnNode) {
    return cachedReturnPaths.computeIfAbsent(returnNode, this::computeAllPathsToReturn);
  }

  private List<WITUpPath> computeAllPathsToReturn(final WITUpNode returnNode) {
    return forwardDFS(findEntryNode(), returnNode);
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
