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
  // type calleeThrows raises. Should be absorved
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

  // same `s == null` NPE arises two ways: this method's own dereference (IMPLICIT) and
  // the callee's (CALLEE_PROPAGATED)
  public static int ownAndCalleeNpe(String s) {
    int own = s.length();
    return own + derefLength(s);
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
}
