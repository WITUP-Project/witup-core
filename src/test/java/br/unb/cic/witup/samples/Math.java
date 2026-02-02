package br.unb.cic.witup.samples;

/**
 * Test class that is passed in as input to SootUpAnalyser for development. Its purpose is not math
 * per se, but to generate cases where we test different ways where parameters and values can come
 * from
 */
public class Math {
  private final double radius;
  public static final double pi = 3.14;
  private static final double MAX_ALLOWED_LN_INPUT = 1.0e308;

  public Math(double radius) {
    this.radius = radius;
  }

  public double circleArea() {
    if (this.radius < 0) {
      throw new RuntimeException("Radius cannot be negative");
    }
    return pi * this.radius * this.radius;
  }

  public int invalidMethodParameter(int x, int y) {
    if (y == 0) {
      throw new RuntimeException("Invalid arguments");
    }
    return x / y;
  }

  public int invalidMethodParameterInConjunctionExpression(int p) {
    if (p < 0 || p > 1) {
      throw new RuntimeException("probability is out of bounds");
    }
    return p;
  }

    // Cenário para global expath: 2 call sites para o mesmo callee, callee com 2 throws
    public double calculateLogInBase(double value, double base) {
        // throw local do caller
        if (base == 1.0) {
            throw new RuntimeException("Log base 1 is undefined");
        }

        double numerator = validatedNaturalLog(value);   // call site #1
        double denominator = validatedNaturalLog(base);  // call site #2
        return numerator / denominator;
    }

    private double validatedNaturalLog(double value) {
        // local expath #1
        if (Double.isInfinite(value)) {
            throw new RuntimeException("Infinite is not allowed");
        }
        // local expath #2
        if (value <= 0.0) {
            throw new RuntimeException("Log of non-positive number");
        }
        return java.lang.Math.log(value);
  }

}
