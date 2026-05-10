package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.ThrowConstraint;
import br.unb.cic.witup.analysis.ThrowSiteKind;
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.JDivExpr;
import sootup.core.jimple.common.expr.JNewArrayExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JRemExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.JInvokeStmt;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JThrowStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.PrimitiveType;
import sootup.core.types.Type;
import sootup.java.core.JavaSootMethod;

/** A graph representation for control property graphs extending JGraphT's DirectedPseudograph. */
public final class WITUpGraph extends DirectedPseudograph<WITUpNode, WITUpEdge> {
  private String methodSignature;
  private JavaSootMethod method;
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

  // Enumerates statements where dereferencing a Local could trigger a JVM-implicit NPE —
  // no `athrow` in the bytecode. Three construct families:
  //   * instance method invocation (`obj.method()`); constructor calls (`<init>`) are
  //     skipped because the receiver was just allocated by `new` and is guaranteed non-null.
  //   * instance field access (`obj.field`), both reads (RHS) and writes (LHS).
  //   * array element access (`arr[i]`), both reads (RHS) and writes (LHS). The base array
  //     reference is the receiver; bounds-check failures on the index are AIOOBE, handled
  //     separately in step 2.4.b.
  // Consumed by MethodSummariser when emitImplicitExceptions is on, to synthesise
  // IMPLICIT-kind ExceptionPaths.
  public List<ImplicitNpeReceiverSite> getImplicitNpeReceiverSites() {
    List<ImplicitNpeReceiverSite> sites = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (!(n instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      Stmt stmt = stmtNode.getStmt();
      Local invokeBase = instanceInvokeReceiver(stmt);
      if (invokeBase != null) {
        sites.add(new ImplicitNpeReceiverSite(n, invokeBase));
      }
      Local fieldBase = instanceFieldReceiver(stmt);
      if (fieldBase != null) {
        sites.add(new ImplicitNpeReceiverSite(n, fieldBase));
      }
      Local arrayBase = arrayAccessBase(stmt);
      if (arrayBase != null) {
        sites.add(new ImplicitNpeReceiverSite(n, arrayBase));
      }
    }
    return sites;
  }

  private static Local instanceInvokeReceiver(final Stmt stmt) {
    AbstractInvokeExpr invoke = invokeExprOf(stmt);
    if (!(invoke instanceof AbstractInstanceInvokeExpr instanceInvoke)) {
      return null;
    }
    if ("<init>".equals(invoke.getMethodSignature().getName())) {
      return null;
    }
    return instanceInvoke.getBase();
  }

  private static Local instanceFieldReceiver(final Stmt stmt) {
    if (!(stmt instanceof JAssignStmt assign)) {
      return null;
    }
    if (assign.getRightOp() instanceof JInstanceFieldRef rhs) {
      return (Local) rhs.getBase();
    }
    if (assign.getLeftOp() instanceof JInstanceFieldRef lhs) {
      return (Local) lhs.getBase();
    }
    return null;
  }

  private static Local arrayAccessBase(final Stmt stmt) {
    JArrayRef ref = arrayRefOf(stmt);
    return ref == null ? null : ref.getBase();
  }

  private static JArrayRef arrayRefOf(final Stmt stmt) {
    if (!(stmt instanceof JAssignStmt assign)) {
      return null;
    }
    if (assign.getRightOp() instanceof JArrayRef rhs) {
      return rhs;
    }
    if (assign.getLeftOp() instanceof JArrayRef lhs) {
      return lhs;
    }
    return null;
  }

  // True if this node has an outgoing exceptional CFG edge that lands on a catch handler
  // — i.e. the stmt is inside a try block whose handler is part of the same method. The
  // rollup phase uses this to distinguish unguarded calls (handled in 3.3) from calls
  // that need catch-type matching (3.4).
  public boolean hasInScopeCatchHandler(final WITUpNode node) {
    for (WITUpEdge edge : outgoingEdgesOf(node)) {
      if (edge instanceof ExceptionalCFGEdge && edge.getTarget() instanceof CaughtExceptionNode) {
        return true;
      }
    }
    return false;
  }

  // Enumerates Jimple call sites: each entry carries the call's WITUpNode, the callee's
  // soot-format signature, and the actuals in the order MethodSummariser.buildFormals()
  // expects (params first, receiver last for instance calls). Consumed by the rollup phase
  // to compose callee escape sets into the caller's summary. Dynamic invokes (lambdas,
  // method handles) are skipped — they don't have a static method signature in the usual
  // sense; SymbolicConstraintGenerator's tryResolveLambda handles them separately.
  public List<MethodCallSite> getCallSites() {
    List<MethodCallSite> sites = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (!(n instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      AbstractInvokeExpr invoke = invokeExprOf(stmtNode.getStmt());
      if (invoke == null) {
        continue;
      }
      if (!(invoke instanceof AbstractInstanceInvokeExpr || invoke instanceof JStaticInvokeExpr)) {
        continue;
      }
      List<Immediate> actuals = new ArrayList<>();
      for (Immediate arg : invoke.getArgs()) {
        actuals.add(arg);
      }
      if (invoke instanceof AbstractInstanceInvokeExpr inst) {
        actuals.add(inst.getBase());
      }
      sites.add(new MethodCallSite(n, invoke.getMethodSignature().toString(), actuals));
    }
    return sites;
  }

  // Enumerates integer `/` and `%` operations for ArithmeticException synthesis. The
  // divisor Immediate carries enough info for MethodSummariser to build the predicate
  // `divisor == 0`. Floating-point divisions are skipped — they produce ±Infinity or NaN
  // rather than raising the exception.
  public List<ImplicitArithmeticSite> getImplicitArithmeticSites() {
    List<ImplicitArithmeticSite> sites = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (!(n instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      Immediate divisor = integerDivisor(assign.getRightOp());
      if (divisor == null) {
        continue;
      }
      sites.add(new ImplicitArithmeticSite(n, divisor));
    }
    return sites;
  }

  private static Immediate integerDivisor(final Object expr) {
    if (expr instanceof JDivExpr div && isIntegerType(div.getType())) {
      return div.getOp2();
    }
    if (expr instanceof JRemExpr rem && isIntegerType(rem.getType())) {
      return rem.getOp2();
    }
    return null;
  }

  private static boolean isIntegerType(final Type t) {
    return t instanceof PrimitiveType.IntType || t instanceof PrimitiveType.LongType;
  }

  // Enumerates `new T[n]` allocation sites for NegativeArraySizeException synthesis. The
  // size Immediate carries enough info for MethodSummariser to build the predicate
  // `size < 0`. Single-dimensional only — multi-dim (JNewMultiArrayExpr) lands later.
  public List<ImplicitNegativeArraySizeSite> getImplicitNegativeArraySizeSites() {
    List<ImplicitNegativeArraySizeSite> sites = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (!(n instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      if (!(stmtNode.getStmt() instanceof JAssignStmt assign)) {
        continue;
      }
      if (!(assign.getRightOp() instanceof JNewArrayExpr newArray)) {
        continue;
      }
      sites.add(new ImplicitNegativeArraySizeSite(n, newArray.getSize()));
    }
    return sites;
  }

  // Enumerates array element accesses for AIOOBE-bounds synthesis. Each site carries the
  // array base and the index Immediate; MethodSummariser builds the bounds-check predicate
  // `arr != null AND (i < 0 || i >= arr.length)`. The non-null conjunct prevents the AIOOBE
  // witness from overlapping with NPE.
  public List<ImplicitAioobeSite> getImplicitAioobeSites() {
    List<ImplicitAioobeSite> sites = new ArrayList<>();
    for (WITUpNode n : this.vertexSet()) {
      if (!(n instanceof SimpleNode sn)) {
        continue;
      }
      if (!(sn.getNode() instanceof StmtGraphNode stmtNode)) {
        continue;
      }
      JArrayRef ref = arrayRefOf(stmtNode.getStmt());
      if (ref == null) {
        continue;
      }
      sites.add(new ImplicitAioobeSite(n, ref.getBase(), ref.getIndex()));
    }
    return sites;
  }

  private static AbstractInvokeExpr invokeExprOf(final Stmt stmt) {
    if (stmt instanceof JInvokeStmt invokeStmt) {
      return invokeStmt.getInvokeExpr().orElse(null);
    }
    if (stmt instanceof JAssignStmt assign
        && assign.getRightOp() instanceof AbstractInvokeExpr invoke) {
      return invoke;
    }
    return null;
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
    // IdentityHashMap-backed set: edges are unique singletons in the graph, so identity
    // comparison is correct, and the backing IdentityHashMap stores entries directly in a
    // flat Object[] (no per-entry Node allocation). Eliminates the HashMap.Node churn that
    // dominated young-gen allocation on big methods. Stays a Set because maxEdgeTraversals
    // is currently 1; if that ever needs to grow this collapses back to a count map.
    Set<WITUpEdge> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    List<WITUpNode> pathNodes = new ArrayList<>();
    List<WITUpEdge> pathEdges = new ArrayList<>();
    pathNodes.add(end);
    backDFS(start, end, visited, pathNodes, pathEdges, result);
    return result;
  }

  // Tracks per-edge traversal so a join node (e.g. a loop header) can be entered from its
  // forward-edge predecessor AND re-entered from a back-edge on the same path. With each
  // edge visitable at most once per path, the body is symbolically unrolled exactly once
  // — sufficient for the catch-block-inside-a-loop case (FileUtils#cleanDirectory). To
  // widen the unrolling (cost: (k+1)^loop-depth more paths per method), swap the visited
  // Set back to a Map<WITUpEdge, Integer> with a count cap.
  private void backDFS(
      final WITUpNode start,
      final WITUpNode current,
      final Set<WITUpEdge> visited,
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
      if (!visited.add(edge)) {
        continue;
      }
      WITUpNode pred = edge.getSource();
      pathNodes.add(pred);
      pathEdges.add(edge);
      backDFS(start, pred, visited, pathNodes, pathEdges, witUpPaths);
      pathNodes.removeLast();
      pathEdges.removeLast();
      visited.remove(edge);
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
    List<WITUpEdge> forwardEdges = path.forwardEdges();
    List<ThrowConstraint> throwConstraints = new ArrayList<>(forwardEdges.size());
    for (int i = 0; i < forwardEdges.size(); i++) {
      WITUpEdge e = forwardEdges.get(i);
      if (e instanceof BooleanCFGEdge boolEdge) {
        throwConstraints.add(new ThrowConstraint(e.getSource(), boolEdge.getCondition(), i));
      } else if (e instanceof ExceptionalCFGEdge) {
        throwConstraints.add(new ThrowConstraint(e.getTarget(), true, i));
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

  // Single source of truth for "is this node the source of a caught exception reference?"
  // Shared by SymbolicConstraintGenerator (per-source check, used to collapse stack temps to
  // SymCaughtExceptionRef) and classifyThrowSite (transitive walk for rethrow detection).
  // Returns the underlying JCaughtExceptionRef or null.
  public static JCaughtExceptionRef caughtExceptionRefOf(final WITUpNode src) {
    if (src instanceof CaughtExceptionNode catchNode) {
      return catchNode.getCaughtExceptionRef();
    }
    if (src instanceof SimpleNode sn
        && sn.getNode() instanceof StmtGraphNode stmtNode
        && stmtNode.getStmt() instanceof JIdentityStmt identity
        && identity.getRightOp() instanceof JCaughtExceptionRef ref) {
      return ref;
    }
    return null;
  }

  // RETHROW iff the throw operand's DDG ancestry includes a caught-exception source;
  // otherwise DIRECT_ATHROW. Path-insensitive and conservative — a throw whose operand
  // could be either a fresh object or a caught reference (e.g. a phi-like join) is
  // labelled RETHROW. The transitive walk is bounded by the method's DDG.
  public ThrowSiteKind classifyThrowSite(final ThrowStatementNode throwNode) {
    List<DataDependencyEdge> seedEdges = getIncomingDDGEdges(throwNode);
    if (seedEdges.isEmpty()) {
      return ThrowSiteKind.DIRECT_ATHROW;
    }
    Set<WITUpNode> visited = new HashSet<>();
    Deque<WITUpNode> worklist = new ArrayDeque<>();
    for (DataDependencyEdge e : seedEdges) {
      worklist.push(getEdgeSource(e));
    }
    while (!worklist.isEmpty()) {
      WITUpNode src = worklist.pop();
      if (!visited.add(src)) {
        continue;
      }
      if (caughtExceptionRefOf(src) != null) {
        return ThrowSiteKind.RETHROW;
      }
      for (DataDependencyEdge e : getIncomingDDGEdges(src)) {
        worklist.push(getEdgeSource(e));
      }
    }
    return ThrowSiteKind.DIRECT_ATHROW;
  }
}
