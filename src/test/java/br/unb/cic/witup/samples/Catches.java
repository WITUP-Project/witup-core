package br.unb.cic.witup.samples;

public class Catches {

  // Conditionally throws — gives us a real "returned normally" branch alongside the throw.
  public static void mayThrow(int x) {
    if (x > 0) {
      throw new IllegalStateException();
    }
  }

  // Two CFG paths reach the throw at the bottom:
  //   (a) mayThrow returned → exception stays null → if-check false → fall through (NOT a throw path).
  //   (b) mayThrow threw → caught → exception = t → if-check true → throw.
  //       For the analyzer to find this path, the catch's def of `exception` must be enumerated.
  public static void simpleCatch(int x) {
    Throwable exception = null;
    try {
      mayThrow(x);
    } catch (Throwable t) {
      exception = t;
    }
    if (null != exception) {
      throw new IllegalArgumentException();
    }
  }

  // Mirrors org.apache.commons.io.FileUtils#cleanDirectory shape: a try/catch inside a
  // for-each loop, with the caught throwable assigned to a variable used after the loop.
  public static void loopCatch(int[] xs) {
    Throwable exception = null;
    for (int x : xs) {
      try {
        mayThrow(x);
      } catch (Throwable t) {
        exception = t;
      }
    }
    if (null != exception) {
      throw new IllegalArgumentException();
    }
  }
}
