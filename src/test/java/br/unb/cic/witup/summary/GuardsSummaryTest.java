package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.analysis.ThrowSiteKind;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GuardsSummaryTest {

  private static final String PKG = "<br.unb.cic.witup.samples.Guards: ";

  private static List<ExceptionPath> npePathsOf(final String signature) {
    MethodSummary summary =
        TestAnalysisContext.getImplicitAnalyser().analyseMethod(signature).summary();
    return summary.exceptionPaths().stream()
        .filter(p -> p.getThrowSiteKind() == ThrowSiteKind.IMPLICIT)
        .filter(p -> "java.lang.NullPointerException".equals(p.getExceptionQualifiedName()))
        .toList();
  }

  @Test
  public void aGuardedDereferenceCarriesTheGuardThatReachedIt() {
    List<ExceptionPath> paths = npePathsOf(PKG + "int guardedDeref(java.lang.String)>");

    assertEquals(1, paths.size());
    List<String> predicate =
        paths.getFirst().getConstraints().stream().map(c -> c.symExpr().toString()).toList();
    assertEquals(2, predicate.size(), "the guard and the null check: " + predicate);
    assertTrue(
        predicate.toString().contains("name"),
        "both must be about the same value or they cannot meet: " + predicate);
  }

  @Test
  public void anUnguardedDereferenceIsUnchanged() {
    List<ExceptionPath> paths = npePathsOf(PKG + "int unguardedDeref(java.lang.String)>");

    assertEquals(1, paths.size());
    assertEquals(
        1, paths.getFirst().getConstraints().size(), "nothing guards it, so nothing is added");
  }

  @Test
  public void independentGuardsBothReachTheSite() {
    List<ExceptionPath> paths = npePathsOf(PKG + "char nestedGuards(java.lang.String,int)>");

    assertEquals(1, paths.size());
    assertEquals(
        3,
        paths.getFirst().getConstraints().size(),
        "two guards and the null check: " + paths.getFirst().getConstraints());
  }
}
