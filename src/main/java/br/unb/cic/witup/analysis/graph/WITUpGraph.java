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
import sootup.core.jimple.common.stmt.Stmt;
import sootup.java.core.JavaSootMethod;

/** A graph representation for control property graphs extending JGraphT's DirectedPseudograph. */
public final class WITUpGraph extends DirectedPseudograph<WITUpNode, WITUpEdge> {
  private String methodSignature;
  private JavaSootMethod method;
  private String dot;
  private WITUpNode entryNode;
  private final Map<WITUpNode, List<WITUpPath>> cachedConstraintPaths = new HashMap<>();
  private final Map<WITUpNode, List<WITUpPath>> cachedReturnPaths = new HashMap<>();
  private Map<WITUpNode, List<WITUpEdge>> cfgIncoming;
  private Map<WITUpNode, List<WITUpEdge>> cfgOutgoing;

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
   * @param method JavaSootMethod
   * @return the converted WITUpGraph
   */
  public static WITUpGraph fromPropertyGraph(final PropertyGraph pg, final JavaSootMethod method) {
    WITUpGraph graph = new WITUpGraph();
    Map<PropertyGraphNode, WITUpNode> cachedNodes = new HashMap<>();
    graph.methodSignature = method.getSignature().toString();
    graph.method = method;
    graph.dot = pg.toDotGraph();

    // SootUp's CPG creators emit an empty PropertyGraph for trivial single-statement
    // methods (e.g. `return CONST;`). Recover the statements directly from the body so
    // the summariser can still see the return node.
    if (pg.getNodes().isEmpty()) {
      for (Stmt stmt : method.getBody().getStmts()) {
        StmtGraphNode synthetic = new StmtGraphNode(stmt);
        WITUpNode n = cachedNodes.computeIfAbsent(synthetic, WITUpGraph::createNode);
        graph.addVertex(n);
      }
    }
    for (PropertyGraphNode node : pg.getNodes()) {
      WITUpNode n = cachedNodes.computeIfAbsent(node, WITUpGraph::createNode);
      graph.addVertex(n);
    }
    for (PropertyGraphEdge edge : pg.getEdges()) {
      WITUpNode source = cachedNodes.computeIfAbsent(edge.getSource(), WITUpGraph::createNode);
      WITUpNode target = cachedNodes.computeIfAbsent(edge.getDestination(), WITUpGraph::createNode);
      graph.addVertex(source);
      graph.addVertex(target);
      switch (edge) {
        case DdgEdge ignored ->
            graph.addEdge(source, target, new DataDependencyEdge(source, target));
        case CdgEdge ignored ->
            graph.addEdge(source, target, new ControlDependencyEdge(source, target));
        case IfTrueCfgEdge ignored ->
            graph.addEdge(source, target, new BooleanCFGEdge(source, target, true));
        case IfFalseCfgEdge ignored ->
            graph.addEdge(source, target, new BooleanCFGEdge(source, target, false));
        case NormalCfgEdge ignored -> graph.addEdge(source, target, new CFGEdge(source, target));
        case GotoCfgEdge ignored -> graph.addEdge(source, target, new GotoCFGEdge(source, target));
        case ExceptionalCfgEdge ignored ->
            graph.addEdge(source, target, new ExceptionalCFGEdge(source, target));
        case SwitchCfgEdge ignored ->
            graph.addEdge(source, target, new SwitchCFGEdge(source, target));
        default ->
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
    List<WITUpNode> throwNodes = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (n instanceof ThrowStatementNode) {
        throwNodes.add(n);
      }
    }
    return throwNodes;
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

  private List<WITUpPath> backwardDFS(final WITUpNode start, final WITUpNode end) {
    List<WITUpPath> result = new ArrayList<>();
    Set<WITUpEdge> visitedEdges = new HashSet<>();
    List<WITUpNode> pathNodes = new ArrayList<>();
    List<WITUpEdge> pathEdges = new ArrayList<>();
    pathNodes.add(end);
    backDFS(start, end, visitedEdges, pathNodes, pathEdges, result);
    return result;
  }

  // Tracks visited *edges* rather than visited nodes so a join node (e.g. a loop header)
  // can be entered from one predecessor edge and re-entered from a back-edge on the same
  // path. Per-edge cycle protection is sufficient: a path cannot traverse the same edge
  // twice, so termination is preserved without forbidding legitimate back-edge revisits
  // that the catch-block-inside-a-loop case (commons-io FileUtils#cleanDirectory) needs.
  private void backDFS(
      final WITUpNode start,
      final WITUpNode current,
      final Set<WITUpEdge> visitedEdges,
      final List<WITUpNode> pathNodes,
      final List<WITUpEdge> pathEdges,
      final List<WITUpPath> witUpPaths) {
    if (current.equals(start)) {
      witUpPaths.add(new WITUpPath(new ArrayList<>(pathNodes), new ArrayList<>(pathEdges)));
      return;
    }

    List<WITUpEdge> incoming = incomingCfgEdges(current);
    for (int i = incoming.size() - 1; i >= 0; i--) {
      WITUpEdge edge = incoming.get(i);
      if (!visitedEdges.add(edge)) {
        continue;
      }
      WITUpNode pred = edge.getSource();
      pathNodes.add(pred);
      pathEdges.add(edge);
      backDFS(start, pred, visitedEdges, pathNodes, pathEdges, witUpPaths);
      pathNodes.removeLast();
      pathEdges.removeLast();
      visitedEdges.remove(edge);
    }
  }

  private List<WITUpPath> computePaths(final WITUpNode start, final WITUpNode end) {
    List<WITUpPath> witUpPaths = new ArrayList<>();
    Deque<WITUpPath> stack = new ArrayDeque<>();
    stack.push(new WITUpPath(new ArrayList<>(List.of(start)), new ArrayList<>()));
    while (!stack.isEmpty()) {
      WITUpPath current = stack.pop();
      WITUpNode tail = current.nodes().getLast();

      if (tail.equals(end)) {
        witUpPaths.add(current);
        continue;
      }

      for (WITUpEdge edge : outgoingCfgEdges(tail)) {
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
    return witUpPaths;
  }

  public List<WITUpPath> getThrowPaths(final WITUpNode throwNode) {
    return cachedConstraintPaths.computeIfAbsent(throwNode, this::computeThrowPaths);
  }

  private List<WITUpPath> computeThrowPaths(final WITUpNode throwNode) {
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
    }
    return pathsWithConstraints;
  }

  private void buildCFGAdjacencies() {
    if (cfgIncoming != null) {
      return;
    }
    cfgIncoming = new HashMap<>();
    cfgOutgoing = new HashMap<>();
    for (WITUpEdge edge : this.edgeSet()) {
      if (edge instanceof CFGEdge) {
        cfgIncoming.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge);
        cfgOutgoing.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
      }
    }
  }

  private List<WITUpEdge> incomingCfgEdges(final WITUpNode node) {
    buildCFGAdjacencies();
    return cfgIncoming.getOrDefault(node, List.of());
  }

  private List<WITUpEdge> outgoingCfgEdges(final WITUpNode node) {
    buildCFGAdjacencies();
    return cfgOutgoing.getOrDefault(node, List.of());
  }

  // usually the entry node is a JIdentityStmt, but its
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
        break;
      }
    }
    return entryNode;
  }

  public List<ThrowConstraint> getThrowConstraints(final WITUpPath path) {
    List<ThrowConstraint> throwConstraints = new ArrayList<>();
    for (WITUpEdge e : path.edges().reversed()) {
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
    List<ReturnStatementNode> result = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (n instanceof ReturnStatementNode r) {
        result.add(r);
      }
    }
    return result;
  }

  public List<WITUpPath> getAllPathsToReturn(final WITUpNode returnNode) {
    return cachedReturnPaths.computeIfAbsent(returnNode, this::computePathsToReturn);
  }

  private List<WITUpPath> computePathsToReturn(final WITUpNode returnNode) {
    return computePaths(findEntryNode(), returnNode);
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
