package br.unb.cic.witup.analysis;

public enum ThrowSiteKind {
  DIRECT_ATHROW,
  RETHROW
  // Reserved for the rollup work:
  //   IMPLICIT          — synthesised at receiver/index/divisor/array-length sites for JVM
  //                        implicit exceptions (NPE, AIOOBE, IOOBE, NegativeArraySize,
  //                        ArithmeticException).
  //   CALLEE_PROPAGATED — escapes from a callee that this method does not catch.
}
