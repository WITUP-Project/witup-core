package br.unb.cic.witup.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SummaryCache implements SummaryRepository {
  private final Map<String, MethodSummary> cachedSummaries = new HashMap<>();
  private final Set<String> inProgress = new HashSet<>();

  @Override
  public Optional<MethodSummary> getSummary(final String methodSignature) {
    return Optional.ofNullable(cachedSummaries.get(methodSignature));
  }

  @Override
  public void putSummary(final String methodSignature, final MethodSummary summary) {
    cachedSummaries.put(methodSignature, summary);
    inProgress.remove(methodSignature);
  }

  @Override
  public boolean isInProgress(final String methodSignature) {
    return inProgress.contains(methodSignature);
  }

  @Override
  public void markInProgress(final String methodSignature) {
    inProgress.add(methodSignature);
  }
}
