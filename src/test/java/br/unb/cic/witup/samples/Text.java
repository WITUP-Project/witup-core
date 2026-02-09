package br.unb.cic.witup.samples;

public class Text {
  public boolean invalidString(String s) {
    if (s == "abc") {
      throw new RuntimeException("Invalid string value");
    }
    return true;
  }

  public boolean invalidStringLength(String s) {
    if (s.length() == 0) {
      throw new RuntimeException("Invalid string length");
    }
    return true;
  }

  public boolean invalidEmptyString(String s) {
    if (s.isEmpty()) {
      throw new RuntimeException("Invalid string length");
    }
    return true;
  }
}
