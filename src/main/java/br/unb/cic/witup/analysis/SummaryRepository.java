package br.unb.cic.witup.analysis;

import java.util.Optional;

public interface SummaryRepository {
  Optional<MethodSummary> getSummary(String methodSignature);

  void putSummary(String methodSignature, MethodSummary summary);

  boolean isInProgress(String methodSignature);

  void markInProgress(String methodSignature);
}
