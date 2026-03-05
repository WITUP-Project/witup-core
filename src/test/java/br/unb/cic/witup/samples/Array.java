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

  public String getStringElement(String[] arr, int i) {
    if (arr[0] == "abc") {
      throw new IllegalArgumentException("string is empty");
    }
    return arr[i];
  }

  public Object getObjectElement(Object[] arr, int i) {
    if (arr[0] == "abc") {
      throw new IllegalArgumentException("string is empty");
    }
    return arr[i];
  }

  public class MyObject {
    public int value;
  }

  public MyObject getObjectFromArray(MyObject[] arr, int i) {
    if (arr[0].value > 10) {
      throw new IllegalArgumentException("value too large");
    }
    return arr[i];
  }
}
