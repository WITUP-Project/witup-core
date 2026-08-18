package br.unb.cic.witup.samples;

public class Calls {
  public static void calleeMayThrow(int x) {
    if (x < 0) {
      throw new IllegalArgumentException();
    }
  }

  // Single unguarded call. No try/catch — `calleeMayThrow`'s IllegalArgumentException must
  // escape this method as a CALLEE_PROPAGATED ExceptionPath.
  public static void unguardedCalleeThrow(int x) {
    calleeMayThrow(x);
  }

  // Same call as unguardedCalleeThrow, but wrapped in a try/catch that catches the exact
  // type calleeThrows raises. The rollup must absorb it — no CALLEE_PROPAGATED path emitted.
  public static void caughtCalleeThrow(int x) {
    try {
      calleeMayThrow(x);
    } catch (IllegalArgumentException e) {
      // absorbed
    }
  }
}
