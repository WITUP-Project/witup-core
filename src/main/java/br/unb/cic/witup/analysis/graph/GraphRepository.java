package br.unb.cic.witup.analysis.graph;

import java.util.Optional;

public interface GraphRepository {
  Optional<WITUpGraph> getGraph(String methodSignature);
}
