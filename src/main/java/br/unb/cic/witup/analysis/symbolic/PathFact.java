package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * One way of arriving at a program point: what had to hold to get here, what each local holds when
 * you do, and how many times each loop was gone round on the way.
 */
public final class PathFact {
  public static final int THIRTY_ONE = 31;
  private final PathCondition pc;
  private final Map<String, SymExpr> env;
  private final int[] unrolls;
  private final boolean widened;

  private PathFact(
      final PathCondition pc,
      final Map<String, SymExpr> env,
      final int[] unrolls,
      final boolean widened) {
    this.pc = pc;
    this.env = env;
    this.unrolls = unrolls;
    this.widened = widened;
  }

  public static PathFact initial(final int componentCount) {
    return new PathFact(PathCondition.EMPTY, new HashMap<>(), new int[componentCount], false);
  }

  public PathCondition pc() {
    return pc;
  }

  /** Track bindings. Callers must not retain or mutate it. */
  public Map<String, SymExpr> env() {
    return env;
  }

  public boolean isWidened() {
    return widened;
  }

  public int unrollsOf(final int sccId) {
    return sccId >= 0 && sccId < unrolls.length ? unrolls[sccId] : 0;
  }

  public PathFact withConstraint(final SymbolicConstraint constraint) {
    return new PathFact(pc.cons(constraint), copyEnv(), unrolls.clone(), widened);
  }

  public PathFact withBinding(final String name, final SymExpr value) {
    Map<String, SymExpr> next = copyEnv();
    next.put(name, value);
    return new PathFact(pc, next, unrolls.clone(), widened);
  }

  public PathFact withUnroll(final int sccId) {
    int[] next = unrolls.clone();
    next[sccId]++;
    return new PathFact(pc, copyEnv(), next, widened);
  }

  static PathFact collapsed(
      final PathCondition pc, final Map<String, SymExpr> env, final int[] unrolls) {
    return new PathFact(pc, env, unrolls, true);
  }

  private Map<String, SymExpr> copyEnv() {
    return new HashMap<>(env);
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof PathFact other
        && pc.equals(other.pc)
        && env.equals(other.env)
        && Arrays.equals(unrolls, other.unrolls)
        && widened == other.widened;
  }

  @Override
  public int hashCode() {
    return THIRTY_ONE * (THIRTY_ONE * pc.hashCode() + env.hashCode()) + Arrays.hashCode(unrolls);
  }

  @Override
  public String toString() {
    return "PathFact" + pc.toList() + env;
  }
}
