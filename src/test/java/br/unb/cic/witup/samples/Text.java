package br.unb.cic.witup.samples;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

  public String requireString(Object s) {
    if (!(s instanceof String)) {
      throw new RuntimeException("must be string");
    }
    return (String) s;
  }
}
