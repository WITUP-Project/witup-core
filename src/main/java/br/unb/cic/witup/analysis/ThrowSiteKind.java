package br.unb.cic.witup.analysis;

public enum ThrowSiteKind {
  DIRECT_ATHROW,
  RETHROW,
  // Synthesised at receiver/index/divisor/array-length sites for JVM implicit exceptions
  // (NPE, AIOOBE, NegativeArraySize, ArithmeticException). No `athrow` in the bytecode —
  // the JVM raises the exception itself. Gated by the emitImplicitExceptions knob.
  IMPLICIT
  // Reserved: CALLEE_PROPAGATED — escapes from a callee that this method does not catch.
}
