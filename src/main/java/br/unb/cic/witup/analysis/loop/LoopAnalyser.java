package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.analysis.graph.node.WITUpNode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level coordinator for loop analysis. Detects natural loops in a graph, runs abstract
 * interpretation on each (inner loops first), and maps every loop body node to its summary.
 */
public final class LoopAnalyser {

  private LoopAnalyser() {}

  /**
   * Analyse all loops in the graph and return a mapping from each loop body node to its summary.
   * Returns an empty map for methods without loops.
   */
  public static Map<WITUpNode, LoopSummary> analyse(final WITUpGraph graph) {
    List<NaturalLoop> loops = NaturalLoopDetector.detect(graph);
    if (loops.isEmpty()) {
      return Map.of();
    }

    // analyse inner loops first (smaller body)
    loops.sort(Comparator.comparingInt(l -> l.body().size()));

    Map<WITUpNode, LoopSummary> result = new HashMap<>();
    for (NaturalLoop loop : loops) {
      LoopSummary summary = LoopAbstractInterpreter.analyse(loop, graph);
      for (WITUpNode node : loop.body()) {
        result.put(node, summary);
      }
    }
    return result;
  }
}
