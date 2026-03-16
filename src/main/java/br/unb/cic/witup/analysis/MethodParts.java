package br.unb.cic.witup.analysis;

public record MethodParts(
    String pkg, String clazz, String method, String returnType, String params) {
  public static MethodParts parseSignature(final String sig) {

    String inner = sig.substring(1, sig.length() - 1);

    String[] sides = inner.split(":");
    String classPart = sides[0].trim();
    String methodPart = sides[1].trim();

    int lastDot = classPart.lastIndexOf('.');
    String pkg = classPart.substring(0, lastDot);
    String clazz = classPart.substring(lastDot + 1);

    String returnType = methodPart.substring(0, methodPart.indexOf(' '));

    String nameAndParams = methodPart.substring(methodPart.indexOf(' ') + 1);
    String method = nameAndParams.substring(0, nameAndParams.indexOf('('));

    String params =
        nameAndParams.substring(nameAndParams.indexOf('(') + 1, nameAndParams.lastIndexOf(')'));

    return new MethodParts(pkg, clazz, method, returnType, params);
  }
}
