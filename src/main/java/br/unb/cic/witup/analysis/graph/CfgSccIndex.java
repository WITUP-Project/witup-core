package br.unb.cic.witup.analysis.graph;

import br.unb.cic.witup.analysis.graph.edge.CFGEdge;
import br.unb.cic.witup.analysis.graph.edge.WITUpEdge;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import sootup.codepropertygraph.propertygraph.nodes.StmtGraphNode;
import sootup.core.jimple.common.stmt.Stmt;

/**
 * Strongly-connected components of a method's CFG, in topological order. id's are sorted
 *
 * <p>NEEDS FIX: Since atm SwitchCFGEdge is not a CFGEdge switch successors are invisible here
 * exactly as they are to every other traversal. Consumers must treat a case target as its own root.
 */
public final class CfgSccIndex {
  private final Map<WITUpNode, Integer> idByNode;
  private final List<List<WITUpNode>> componentsInOrder;

  private CfgSccIndex(
      final Map<WITUpNode, Integer> idByNode, final List<List<WITUpNode>> componentsInOrder) {
    this.idByNode = idByNode;
    this.componentsInOrder = componentsInOrder;
  }

  static CfgSccIndex of(final WITUpGraph cpg) {
    DefaultDirectedGraph<WITUpNode, DefaultEdge> projection =
        new DefaultDirectedGraph<>(DefaultEdge.class);
    for (WITUpNode vertex : cpg.vertexSet()) {
      projection.addVertex(vertex);
    }
    for (WITUpEdge edge : cpg.edgeSet()) {
      if (edge instanceof CFGEdge) {
        projection.addEdge(edge.getSource(), edge.getTarget());
      }
    }

    Map<WITUpNode, Integer> position = statementPositions(cpg);
    Graph<Graph<WITUpNode, DefaultEdge>, DefaultEdge> condensation =
        new KosarajuStrongConnectivityInspector<>(projection).getCondensation();

    Comparator<Graph<WITUpNode, DefaultEdge>> byFirstStatement =
        Comparator.comparingInt(component -> earliestPosition(component.vertexSet(), position));

    Map<WITUpNode, Integer> idByNode = new IdentityHashMap<>(cpg.vertexSet().size() * 2);
    List<List<WITUpNode>> componentsInOrder = new ArrayList<>();
    TopologicalOrderIterator<Graph<WITUpNode, DefaultEdge>, DefaultEdge> components =
        new TopologicalOrderIterator<>(condensation, byFirstStatement);
    while (components.hasNext()) {
      List<WITUpNode> members = new ArrayList<>(components.next().vertexSet());
      members.sort(Comparator.comparingInt(node -> position.getOrDefault(node, Integer.MAX_VALUE)));
      int id = componentsInOrder.size();
      for (WITUpNode member : members) {
        idByNode.put(member, id);
      }
      componentsInOrder.add(List.copyOf(members));
    }
    return new CfgSccIndex(idByNode, List.copyOf(componentsInOrder));
  }

  public int sccIdOf(final WITUpNode node) {
    return idByNode.getOrDefault(node, -1);
  }

  public List<List<WITUpNode>> topologicalSccs() {
    return componentsInOrder;
  }

  public boolean isIntraScc(final WITUpEdge edge) {
    if (!(edge instanceof CFGEdge)) {
      return false;
    }
    int source = sccIdOf(edge.getSource());
    return source != -1 && source == sccIdOf(edge.getTarget());
  }

  private static int earliestPosition(
      final Iterable<WITUpNode> nodes, final Map<WITUpNode, Integer> position) {
    int earliest = Integer.MAX_VALUE;
    for (WITUpNode node : nodes) {
      earliest = Math.min(earliest, position.getOrDefault(node, Integer.MAX_VALUE));
    }
    return earliest;
  }

  private static Map<WITUpNode, Integer> statementPositions(final WITUpGraph cpg) {
    Map<Stmt, Integer> byStmt = new HashMap<>();
    List<Stmt> stmts = cpg.getMethod().getBody().getStmts();
    for (int i = 0; i < stmts.size(); i++) {
      byStmt.putIfAbsent(stmts.get(i), i);
    }
    Map<WITUpNode, Integer> position = new IdentityHashMap<>(cpg.vertexSet().size() * 2);
    for (WITUpNode node : cpg.vertexSet()) {
      if (node.getNode() instanceof StmtGraphNode stmtNode) {
        Integer at = byStmt.get(stmtNode.getStmt());
        if (at != null) {
          position.put(node, at);
        }
      }
    }
    return position;
  }
}
