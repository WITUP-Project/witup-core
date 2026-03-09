package br.unb.cic.witup.analysis.summary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SummaryCache {
  private final Map<String, MethodSummary> cache = new HashMap<>();
  private final Set<String> inProgress = new HashSet<>();

  public Optional<MethodSummary> get(final String sig) {
    return Optional.ofNullable(cache.get(sig));
  }

  public void put(final String sig, final MethodSummary summary) {
    cache.put(sig, summary);
    inProgress.remove(sig);
  }

  public boolean isInProgress(final String sig) {
    return inProgress.contains(sig);
  }

  public void markInProgress(final String sig) {
    inProgress.add(sig);
  }

  public int size() {
    return cache.size();
  }
}
