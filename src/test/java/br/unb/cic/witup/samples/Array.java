package br.unb.cic.witup.samples;

public class Array {
  public int getElement(int[] arr, int i) {
    if (arr[i] == 0) {
      throw new IllegalArgumentException("element is zero");
    }
    return arr[i];
  }

  public int checkLength(int[] arr) {
    if (arr.length == 0) {
      throw new IllegalArgumentException("array is empty");
    }
    return arr.length;
  }

  public int[] allocate(int n) {
    if (n < 0) {
      throw new IllegalArgumentException("negative size");
    }
    return new int[n];
  }
}
