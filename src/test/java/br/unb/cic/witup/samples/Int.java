package br.unb.cic.witup.samples;

// test operations on the primitive type int
public class Int {
  public int add(int a, int b) {
    if (a + b > 256) {
      throw new IllegalArgumentException("a + b > 256 overflows");
    }
    return a + b;
  }

  public int greaterThanConstantRhs(int a) {
    if (a < 0) {
      throw new IllegalArgumentException("a must be positive");
    }
    return a;
  }

  public int lesserThanConstantLhs(int a) {
    if (0 > a) {
      throw new IllegalArgumentException("a must be positive");
    }
    return a;
  }

  public int equalsConstantRhs(int a) {
    if (a == 0) {
      throw new IllegalArgumentException("a must not be zero");
    }
    return a;
  }

  public int equalsConstantLhs(int a) {
    if (0 == a) {
      throw new IllegalArgumentException("a must not be zero");
    }
    return a;
  }

  public int negatedLessThanConstantRhs(int a) {
    if (!(a > 0)) {
      throw new IllegalArgumentException("a must be positive");
    }
    return a;
  }

  public int lessThanConstantRhsViaBoolean(int a) {
    boolean invalid = a < 0;
    if (invalid) {
      throw new IllegalArgumentException("a cannot be negative");
    }
    return a;
  }

  public int lessThanConstantRhsViaNegatedBoolean(int a) {
    boolean invalid = a < 0;
    if (!invalid) {
      throw new IllegalArgumentException("a cannot be positive");
    }
    return a;
  }
}
