package br.unb.cic.witup.analysis.symbolic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The conjunction of branch decisions taken to reach a program point, as a persistent list that
 * shares its tail. A branch forks one condition into two paths that agree on everything decided
 * before it. consing keeps one cell per decision no matter how many paths run through it.
 */
public final class PathCondition {
  public static final PathCondition EMPTY = new PathCondition(null, null, 0, 1);

  private static final int HASH_FACTOR = 31;

  private final SymbolicConstraint head;
  private final PathCondition tail;
  private final int length;
  private final int hash;

  private PathCondition(
      final SymbolicConstraint head, final PathCondition tail, final int length, final int hash) {
    this.head = head;
    this.tail = tail;
    this.length = length;
    this.hash = hash;
  }

  /** This condition extended with one more decision. O(1), and does not copy. */
  public PathCondition cons(final SymbolicConstraint constraint) {
    return new PathCondition(
        constraint, this, length + 1, HASH_FACTOR * hash + constraint.hashCode());
  }

  public int length() {
    return length;
  }

  public boolean contains(final SymbolicConstraint constraint) {
    for (PathCondition cell = this; cell.length > 0; cell = cell.tail) {
      if (cell.head.equals(constraint)) {
        return true;
      }
    }
    return false;
  }

  /** Constraints in entry-to-site order. */
  public List<SymbolicConstraint> toList() {
    if (length == 0) {
      return List.of();
    }
    List<SymbolicConstraint> out = new ArrayList<>(length);
    for (PathCondition cell = this; cell.length > 0; cell = cell.tail) {
      out.add(cell.head);
    }
    Collections.reverse(out);
    return out;
  }

  /**
   * The constraints every input agrees on Any tail the inputs already share is reused by identity,
   * so merging deep inside a method costs only the constraints above the fork.
   */
  public static PathCondition intersect(final Collection<PathCondition> conditions) {
    if (conditions.isEmpty()) {
      return EMPTY;
    }
    List<PathCondition> facts = List.copyOf(conditions);
    if (facts.size() == 1) {
      return facts.getFirst();
    }

    PathCondition shared = facts.getFirst();
    for (PathCondition other : facts) {
      shared = deepestSharedCell(shared, other);
    }

    List<SymbolicConstraint> candidates = facts.getFirst().above(shared);
    if (candidates.isEmpty()) {
      return shared;
    }
    List<Set<SymbolicConstraint>> others = new ArrayList<>(facts.size() - 1);
    for (int i = 1; i < facts.size(); i++) {
      others.add(new HashSet<>(facts.get(i).above(shared)));
    }

    PathCondition result = shared;
    for (SymbolicConstraint candidate : candidates) {
      boolean inAll = true;
      for (Set<SymbolicConstraint> other : others) {
        if (!other.contains(candidate)) {
          inAll = false;
          break;
        }
      }
      if (inAll) {
        result = result.cons(candidate);
      }
    }
    return result;
  }

  // Walk both chains down to the deepest cell they are the same object. EMPTY is a singleton and
  // terminates every chain, so this always lands somewhere.
  private static PathCondition deepestSharedCell(final PathCondition a, final PathCondition b) {
    PathCondition left = a;
    PathCondition right = b;
    while (left.length > right.length) {
      left = left.tail;
    }
    while (right.length > left.length) {
      right = right.tail;
    }
    while (left != right) {
      left = left.tail;
      right = right.tail;
    }
    return left;
  }

  // Constraints of this condition that sit above `bound`, entry-first.
  private List<SymbolicConstraint> above(final PathCondition bound) {
    if (this == bound) {
      return List.of();
    }
    List<SymbolicConstraint> out = new ArrayList<>(length - bound.length);
    for (PathCondition cell = this; cell != bound; cell = cell.tail) {
      out.add(cell.head);
    }
    Collections.reverse(out);
    return out;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PathCondition other) || hash != other.hash || length != other.length) {
      return false;
    }
    PathCondition left = this;
    PathCondition right = other;
    while (left != right) {
      if (left.length == 0) {
        return true;
      }
      if (!left.head.equals(right.head)) {
        return false;
      }
      left = left.tail;
      right = right.tail;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return toList().toString();
  }
}
