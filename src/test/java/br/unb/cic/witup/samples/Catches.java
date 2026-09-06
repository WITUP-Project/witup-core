package br.unb.cic.witup.samples;

public class Catches {

  // Conditionally throws — gives us a real "returned normally" branch alongside the throw.
  public static void mayThrow(int x) {
    if (x > 0) {
      throw new IllegalStateException();
    }
  }

  // try-with-resources compiles to a synthetic catch-all handler that closes the resource and
  // rethrows what it caught. It does not swallow anything, so mayThrow's exception must still
  // escape this method. Shape of FileUtils.copyInputStreamToFile.
  public static void tryWithResources(int x, java.io.Closeable resource) throws Exception {
    try (java.io.Closeable open = resource) {
      mayThrow(x);
    }
  }

  // Two CFG paths reach the throw at the bottom:
  //   (a) mayThrow returned → exception stays null → if-check false → fall through (NOT a throw
  // path).
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

  // Same shape as simpleCatch, but rethrows the captured Throwable instead of authoring a
  // fresh exception. The throw operand traces back through DDG to a JCaughtExceptionRef,
  // which is what classifyThrowSite uses to label the path RETHROW.
  public static void simpleRethrow(int x) throws Throwable {
    Throwable exception = null;
    try {
      mayThrow(x);
    } catch (Throwable t) {
      exception = t;
    }
    if (null != exception) {
      throw exception;
    }
  }

  private static void sink(int x) {
    // no-op; exists so the finally block has a body javac must duplicate
  }

  // try/finally. javac compiles the finally body twice: once on the normal path, once into a
  // synthetic handler with `catch_type = any` that rethrows the caught reference. SootUp maps
  // `any` onto java.lang.Throwable, so that handler is indistinguishable by declared type from
  // a source-level `catch (Throwable t)`. The rethrow is not an exception source — whatever
  // mayThrow raises is already reported as its own path — so it must not appear in the summary.
  public static void tryFinally(int x) {
    try {
      mayThrow(x);
    } finally {
      sink(x);
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
