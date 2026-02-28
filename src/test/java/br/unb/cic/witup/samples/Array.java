package br.unb.cic.witup.samples;

public class Array {
  public int getElement(int[] arr, int i) {
    if (arr[i] == 0) {
      throw new IllegalArgumentException("element is zero");
    }
    return arr[i];
  }
}
