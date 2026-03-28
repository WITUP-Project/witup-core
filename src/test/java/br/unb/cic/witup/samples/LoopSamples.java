package br.unb.cic.witup.samples;

public class LoopSamples {

  /** Throw reachable: sum is loop-carried (Top), so sum > 256 is SAT. */
  public void sumThreshold(int[] arr) {
    int sum = 0;
    for (int i = 0; i < arr.length; i++) {
      sum += arr[i];
      if (sum > 256) {
        throw new IllegalArgumentException("sum exceeds threshold");
      }
    }
  }

  /** Throw unreachable: x accumulates 5 per iteration for 10 iterations, x in [0,50]. */
  public void constantAccumulator() {
    int x = 0;
    for (int i = 0; i < 10; i++) {
      x += 5;
    }
    if (x > 100) {
      throw new IllegalArgumentException("x exceeds 100");
    }
  }

  /** Simple counter loop with array element check — throw is reachable. */
  public void inductionVarThrow(int[] arr) {
    for (int i = 0; i < arr.length; i++) {
      if (arr[i] < 0) {
        throw new IllegalArgumentException("negative element");
      }
    }
  }
}
