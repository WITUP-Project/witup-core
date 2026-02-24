// package br.unb.cic.witup.analysis;
//
// import java.util.List;
//
// public final class SymStaticInvoke extends SymExpr {
//  private final String invokeName; // e.g. length
//  private final boolean returnsBoolean;
//  private final List<SymExpr> args;
//
//  public List<SymExpr> getArgs() {
//    return args;
//  }
//
//  public String getInvokeName() {
//    return invokeName;
//  }
//
//  public SymStaticInvoke(final String invokeName, final boolean returnsBoolean, final
// List<SymExpr> args) {
//    this.invokeName = invokeName;
//    this.returnsBoolean = returnsBoolean;
//    this.args = args;
//  }
//
//  @Override
//  public SymExpr substitute(final String invField, final SymExpr replacement) {
//    List<SymExpr> newArgs = args.stream()
//            .map(arg -> arg.substitute(invField, replacement))
//            .toList();
//    return new SymStaticInvoke(invokeName, returnsBoolean, newArgs);
//  }
//
//  @Override
//  public boolean contains(final String varName) {
//    return args.contains(varName);
//  }
//
//  @Override
//  public String toString() {
//    return invokeName;
//  }
//
//  @Override
//  public SymKind kind() {
//    return returnsBoolean ? SymKind.BOOLEAN_METHOD : SymKind.OTHER;
//  }
// }
