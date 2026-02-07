package br.unb.cic.witup.samples;

public class Text {
  public boolean invalidString(String s) {
    if (s == "abc") {
      throw new RuntimeException("Invalid string value");
    }
    return true;
  }
}
