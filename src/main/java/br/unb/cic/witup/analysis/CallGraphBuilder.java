package br.unb.cic.witup.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.views.JavaView;

public final class CallGraphBuilder {
  private final JavaView view;
  private static final Logger log = LoggerFactory.getLogger("CallGraphBuilder");

  public CallGraphBuilder(final JavaView view) {
    this.view = view;
  }

  // we'll start with only keeping track of method signatures and use better types
  // if necessary
  public DefaultDirectedGraph<String, DefaultEdge> build(final Set<String> methodSignatures) {
    List<MethodSignature> entryPoints =
        methodSignatures.stream()
            .map(sig -> view.getIdentifierFactory().parseMethodSignature(sig))
            .collect(Collectors.toList());

    log.debug("Building CHA call graph with {} entry points", entryPoints.size());
    CallGraphAlgorithm cha = new ClassHierarchyAnalysisAlgorithm(view);
    CallGraph cg = cha.initialize(entryPoints);

    DefaultDirectedGraph<String, DefaultEdge> callGraph =
        new DefaultDirectedGraph<>(DefaultEdge.class);

    for (String sig : methodSignatures) {
      callGraph.addVertex(sig);
    }

    for (String sig : methodSignatures) {
      MethodSignature ms = view.getIdentifierFactory().parseMethodSignature(sig);
      cg.callsFrom(ms)
          .forEach(
              callee -> {
                String calleeSig = callee.toString();
                if (methodSignatures.contains(calleeSig)) {
                  callGraph.addEdge(sig, calleeSig);
                }
              });
    }

    return callGraph;
  }

  public List<List<String>> buildAnalysisOrder(final Graph<String, DefaultEdge> callGraph) {
    KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector =
        new KosarajuStrongConnectivityInspector<>(callGraph);

    Graph<Graph<String, DefaultEdge>, DefaultEdge> condensation = inspector.getCondensation();

    TopologicalOrderIterator<Graph<String, DefaultEdge>, DefaultEdge> topoIter =
        new TopologicalOrderIterator<>(condensation);

    List<List<String>> analysisOrder = new ArrayList<>();
    while (topoIter.hasNext()) {
      Graph<String, DefaultEdge> scc = topoIter.next();
      analysisOrder.add(new ArrayList<>(scc.vertexSet()));
    }

    long nonSingletons = analysisOrder.stream().filter(scc -> scc.size() > 1).count();
    log.debug("Non-singleton SCCs (genuine recursion): {}", nonSingletons);
    return analysisOrder;
  }
}
