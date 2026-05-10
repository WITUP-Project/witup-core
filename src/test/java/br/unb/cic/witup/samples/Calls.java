package br.unb.cic.witup.samples;

public class Calls {
  // Conditionally throws — the callee whose summary the rollup will compose.
  public static void calleeThrows(int x) {
    if (x < 0) {
      throw new IllegalArgumentException();
    }
  }

  // Single unguarded call. No try/catch — `calleeThrows`'s IllegalArgumentException must
  // escape this method as a CALLEE_PROPAGATED ExceptionPath.
  public static void unguardedCalleeThrow(int x) {
    calleeThrows(x);
  }
}
