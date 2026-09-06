package br.unb.cic.witup.samples;

public class Calls {
  public static void calleeMayThrow(int x) {
    if (x < 0) {
      throw new IllegalArgumentException();
    }
  }

  // `calleeMayThrow`'s IllegalArgumentException must escape this method
  //  as a CALLEE_PROPAGATED ExceptionPath.
  public static void unguardedCalleeThrow(int x) {
    calleeMayThrow(x);
  }

  // Same call as unguardedCalleeThrow, but wrapped in a try/catch that catches the exact
  // type calleeThrows raises. Should be absorbed
  public static void caughtCalleeThrow(int x) {
    try {
      calleeMayThrow(x);
    } catch (IllegalArgumentException e) {
      // absorbed
    }
  }

  // null argument raises an implicit NPE.
  public static int derefLength(String s) {
    return s.length();
  }

  // same dereference one frame deeper.
  public static int derefIndirect(String s) {
    return derefLength(s);
  }

  // Simplified shape of org.apache.commons.io.FilenameUtils#isExtension, which reaches the
  // same NPE
  public static int sameNpeViaTwoChains(String s) {
    return derefLength(s) + derefIndirect(s);
  }

  // Same `s == null` NPE two ways: the callee's dereference (CALLEE_PROPAGATED) and this method's
  // own (IMPLICIT). The call comes first deliberately. With the dereference first, reaching the
  // call would prove `s` non-null and refute the propagated flow, leaving nothing for the two
  // kinds to collide over — correct, but no longer a test of whether kind separates them.
  // In this order nothing local is proven: that the callee dereferenced `s` is the callee's
  // knowledge, and we do not carry it back to its caller.
  public static int ownAndCalleeNpe(String s) {
    int fromCallee = derefLength(s);
    return fromCallee + s.length();
  }

  // The walker emits a method's own throws before its callee-propagated ones, so the two
  // colliding NPE paths here land at indices 1 and 2, behind the IAE at index 0. Collapsing
  // them must keep the first
  public static int ownThrowThenTwoChains(String s, int n) {
    if (n < 0) {
      throw new IllegalArgumentException();
    }
    return derefLength(s) + derefIndirect(s);
  }

  // Predicate reaches the parameter through a length expression rather than directly, so the
  // parameter reference sits under a SymLength inside the comparison.
  public static void throwIfEmpty(int[] a) {
    if (a.length == 0) {
      throw new IllegalArgumentException();
    }
  }

  // Deliberately different parameter name from throwIfEmpty's `a`, so a composed predicate that
  // still mentions `a` proves the length-wrapped parameter was never substituted.
  public static void callThrowIfEmpty(int[] items) {
    throwIfEmpty(items);
  }

  // Throws only when lo is strictly greater than hi.
  public static void requireOrdered(int lo, int hi) {
    if (lo > hi) {
      throw new IllegalArgumentException();
    }
  }

  // Deliberately different parameter name from derefLength's `s`, so a composed predicate that
  // still mentions `s` proves the callee's local leaked instead of the caller's actual.
  public static int callDerefWithOtherName(String zzz) {
    return derefLength(zzz);
  }

  // Passes the same value for both params, so the composed predicate becomes `x > x`.
  // Infeasible, but only after substitution, and invisible to constant folding since neither
  // side is a constant — so only the solver can refute it.
  public static void alwaysOrdered(int x) {
    requireOrdered(x, x);
  }
}
