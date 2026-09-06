package br.unb.cic.witup.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ExceptionPath;
import br.unb.cic.witup.analysis.MethodSummary;
import br.unb.cic.witup.testinfra.TestAnalysisContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class SwitchesSummaryTest {

  private static final String CLASSIFY =
      "<br.unb.cic.witup.samples.Switches: int classify(java.lang.String,int)>";

  @Test
  public void everySwitchArmThatThrowsIsSummarised() {
    MethodSummary summary = TestAnalysisContext.getAnalyser().analyseMethod(CLASSIFY).summary();

    Set<String> thrown =
        summary.exceptionPaths().stream()
            .map(ExceptionPath::getExceptionQualifiedName)
            .collect(Collectors.toSet());

    assertEquals(
        Set.of(
            "java.lang.IllegalArgumentException",
            "java.lang.IllegalStateException",
            "java.lang.UnsupportedOperationException"),
        thrown,
        "the two switch arms throw as surely as the guard does");
  }

  @Test
  public void switchArmPredicatesCarryTheGuardButNotTheCaseLabel() {
    MethodSummary summary = TestAnalysisContext.getAnalyser().analyseMethod(CLASSIFY).summary();

    List<ExceptionPath> armPaths =
        summary.exceptionPaths().stream()
            .filter(
                p -> !"java.lang.IllegalArgumentException".equals(p.getExceptionQualifiedName()))
            .toList();

    for (ExceptionPath path : armPaths) {
      String predicate = path.getConstraints().toString();
      assertTrue(
          predicate.contains("name"),
          "the guard reaching the switch must survive on the arm: " + predicate);
      // The case label itself is not modelled: getThrowConstraints reads BooleanCFGEdge and
      // ExceptionalCFGEdge only, so a switch edge contributes nothing. The predicate is therefore
      // weaker than the truth (it omits `kind == 1`), which costs precision, not recall.
      assertEquals(
          1, path.getConstraints().size(), "only the guard is modelled today: " + predicate);
    }
  }
}
