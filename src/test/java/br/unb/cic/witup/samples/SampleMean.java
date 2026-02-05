package br.unb.cic.witup.samples;

public final class SampleMean {

    public static double processSample(double sum, int n) {
        if (n > 10_000) {
            throw new RuntimeException("sample too large");
        }
        return meanChecked(sum, n);
    }

    public static double meanChecked(double sum, int n) {
        if (sum == 0.0) {
            throw new RuntimeException("sum cannot be zero (example)");
        }
        return meanCore(sum, n);
    }

    public static double meanCore(double sum, int n) {
        if (n <= 0) {
            throw new RuntimeException("n must be positive");
        }
        return sum / n;
    }
}
