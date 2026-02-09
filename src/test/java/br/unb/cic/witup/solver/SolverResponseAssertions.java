package br.unb.cic.witup.solver;

public class SolverResponseAssertions {
  private SolverResponseAssertions() {}

  public static SolverResponse.SolverPathResult path(
          SolverResponse response, String pathId) {

    return response.getPaths().stream()
            .filter(p -> p.getPathId().equals(pathId))
            .findFirst()
            .orElseThrow(() ->
                    new AssertionError("No solver result for path: " + pathId));
  }

  public static SolverResponse.SolverPathSolution value(
          SolverResponse.SolverPathResult path, String symbol) {

    return path.getSolutions().stream()
            .filter(s -> s.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(() ->
                    new AssertionError(
                            "No value for symbol '" + symbol +
                                    "' in path '" + path.getPathId() + "'"));
  }

  public static boolean booleanValue(SolverResponse.SolverPathResult path, String symbol) {
    return Boolean.parseBoolean(value(path, symbol).getValue());
  }

  public static char charValue(SolverResponse.SolverPathResult path, String symbol) {
    String val = value(path, symbol).getValue();
    if (val.length() != 1) {
      throw new AssertionError(
              "Expected single character for symbol '" + symbol + "' but got: " + val);
    }
    return val.charAt(0);
  }

  public static byte byteValue(SolverResponse.SolverPathResult path, String symbol) {
    return Byte.parseByte(value(path, symbol).getValue());
  }

  public static short shortValue(SolverResponse.SolverPathResult path, String symbol) {
    return Short.parseShort(value(path, symbol).getValue());
  }

  public static int intValue(SolverResponse.SolverPathResult path, String symbol) {
    return Integer.parseInt(value(path, symbol).getValue());
  }

  public static long longValue(SolverResponse.SolverPathResult path, String symbol) {
    return Long.parseLong(value(path, symbol).getValue());
  }

  public static float floatValue(SolverResponse.SolverPathResult path, String symbol) {
    return Float.parseFloat(value(path, symbol).getValue());
  }

  public static double doubleValue(SolverResponse.SolverPathResult path, String symbol) {
    return Double.parseDouble(value(path, symbol).getValue());
  }

  public static String stringValue(SolverResponse.SolverPathResult path, String symbol) {
    return value(path, symbol).getValue();
  }
}
