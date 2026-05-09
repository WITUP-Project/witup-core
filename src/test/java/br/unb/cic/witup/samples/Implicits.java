package br.unb.cic.witup.samples;

public class Implicits {
  // Single instance method invocation. Receiver `o` is a parameter, so it could be null
  // at runtime. The JVM raises an implicit NPE — no `athrow` in the bytecode — which is
  // exactly what the implicit-exception synthesis is meant to capture.
  public static int receiverNpe(Object o) {
    return o.hashCode();
  }

  public static class Box {
    public int x;
  }

  // Single instance field read. Receiver `b` is a parameter, so the JVM raises an implicit
  // NPE before evaluating `b.x` if `b` is null.
  public static int fieldNpe(Box b) {
    return b.x;
  }

  // Single array element read. The array `arr` is a parameter, so an implicit NPE fires if
  // `arr` is null. Index-bounds (AIOOBE) is a separate predicate, synthesised in 2.4.b.
  public static int arrayDeref(int[] arr) {
    return arr[0];
  }

  // Single `new T[n]` allocation. If `n` is negative at runtime, the JVM raises
  // NegativeArraySizeException — no `athrow` in the bytecode.
  public static int[] negativeArraySize(int n) {
    return new int[n];
  }

  // Single integer division. If `b` is zero at runtime, the JVM raises ArithmeticException
  // — no `athrow` in the bytecode. Floating-point divides by zero produce ±Infinity / NaN
  // rather than the exception, so are excluded from implicit-arithmetic detection.
  public static int divByZero(int a, int b) {
    return a / b;
  }
}
