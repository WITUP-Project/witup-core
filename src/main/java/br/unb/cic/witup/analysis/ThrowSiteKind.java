package br.unb.cic.witup.analysis;

public enum ThrowSiteKind {
  DIRECT_ATHROW,
  RETHROW,
  // Synthesised at receiver/index/divisor/array-length sites for JVM implicit exceptions
  // (NPE, AIOOBE, NegativeArraySize, ArithmeticException). No `athrow` in the bytecode —
  // the JVM raises the exception itself. Gated by the emitImplicitExceptions knob.
  IMPLICIT,
  // Escapes from a callee that this method does not catch. The caller's predicate is its
  // path conditions to the call site conjoined with the callee's predicate (formals→actuals
  // substituted). Provenance is non-empty: [calleeSig, ...calleeProvenance].
  CALLEE_PROPAGATED
}
