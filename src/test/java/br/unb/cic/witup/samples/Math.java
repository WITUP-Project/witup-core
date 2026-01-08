package br.unb.cic.witup.samples;

/**
 * Test class that is passed in as input to SootUpAnalyser for development. Its
 * purpose is not math per se, but to generate cases where we test different
 * ways where parameters and values can come from
 */
public class Math {
    private final double radius;
    public static final double pi = 3.14;

    public Math(double radius) {
        this.radius = radius;
    }

    public double invalidClassField() {
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
}