package br.unb.cic.witup.samples;

public class Bool {
  // From apache commons used exclusively for testing. Hopefully we don't need
  // to delete for licensing reasons?
  public static boolean toBoolean(
      final Integer value, final Integer trueValue, final Integer falseValue) {
    if (value == null) {
      if (trueValue == null) {
        return true;
      }
      if (falseValue == null) {
        return false;
      }
    } else if (value.equals(trueValue)) {
      return true;
    } else if (value.equals(falseValue)) {
      return false;
    }
    throw new IllegalArgumentException("The Integer did not match either specified value");
  }
}
