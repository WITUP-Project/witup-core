package br.unb.cic.witup.analysis.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import br.unb.cic.witup.analysis.symbolic.expr.SymVar;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PathConditionTest {

  private static SymbolicConstraint constraint(final String name, final boolean truthValue) {
    return new SymbolicConstraint(SymVar.fresh(name, SymKind.BOOLEAN), truthValue);
  }

  @Test
  public void constraintsReadInEntryToSiteOrder() {
    SymbolicConstraint first = constraint("guard", true);
    SymbolicConstraint second = constraint("bound", false);

    PathCondition pc = PathCondition.EMPTY.cons(first).cons(second);

    assertEquals(2, pc.length());
    assertEquals(List.of(first, second), pc.toList());
    assertEquals(0, PathCondition.EMPTY.length());
    assertEquals(List.of(), PathCondition.EMPTY.toList());
  }

  @Test
  public void forksShareTheirCommonPrefix() {
    PathCondition base = PathCondition.EMPTY.cons(constraint("outer", true));
    PathCondition left = base.cons(constraint("inner", true));
    PathCondition right = base.cons(constraint("inner", false));

    assertSame(
        base,
        PathCondition.intersect(List.of(left, right)),
        "the shared tail must be returned by identity, not rebuilt");
  }

  @Test
  public void intersectKeepsOnlyWhatEveryBranchAgreesOn() {
    SymbolicConstraint shared = constraint("shared", true);
    PathCondition left = PathCondition.EMPTY.cons(shared).cons(constraint("onlyLeft", true));
    PathCondition right = PathCondition.EMPTY.cons(shared).cons(constraint("onlyRight", true));

    assertEquals(List.of(shared), PathCondition.intersect(List.of(left, right)).toList());
  }

  @Test
  public void intersectKeepsAgreementReachedAfterADivergence() {
    SymbolicConstraint entry = constraint("entry", true);
    SymbolicConstraint late = constraint("late", true);
    PathCondition left = PathCondition.EMPTY.cons(entry).cons(constraint("c", true)).cons(late);
    PathCondition right = PathCondition.EMPTY.cons(entry).cons(constraint("c", false)).cons(late);

    assertEquals(
        List.of(entry, late),
        PathCondition.intersect(List.of(left, right)).toList(),
        "intersection is by membership, not longest common prefix");
  }

  @Test
  public void intersectIsDrivenByStructureNotIdentity() {
    SymbolicConstraint a = constraint("a", true);
    SymbolicConstraint b = constraint("b", false);
    PathCondition one = PathCondition.EMPTY.cons(a).cons(b);
    PathCondition other = PathCondition.EMPTY.cons(a).cons(b);

    assertEquals(one, other);
    assertEquals(one.hashCode(), other.hashCode());
    assertEquals(List.of(a, b), PathCondition.intersect(List.of(one, other)).toList());
  }

  @Test
  public void disjointBranchesIntersectToNothing() {
    PathCondition left = PathCondition.EMPTY.cons(constraint("left", true));
    PathCondition right = PathCondition.EMPTY.cons(constraint("right", true));

    assertSame(PathCondition.EMPTY, PathCondition.intersect(List.of(left, right)));
    assertNotEquals(left, right);
  }
}
