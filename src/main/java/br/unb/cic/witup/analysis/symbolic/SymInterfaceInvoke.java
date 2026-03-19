package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.types.PrimitiveType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SymInterfaceInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String signature; // e.g. length
  private final boolean returnsBoolean;
  private final List<SymExpr> args;

  // we have access to getArgs, getArgCount, getMethodSignature, getType, getUses

  public SymExpr getBase() {
    return base;
  }

  public static SymExpr fromInterfaceInvokeExpr(final JInterfaceInvokeExpr e) {
    SymExpr base = fromJimple(e.getBase());
    String invokedMethodName = e.getMethodSignature().getSubSignature().getName();
    boolean returnsBoolean =
            e.getMethodSignature().getSubSignature().getType() instanceof PrimitiveType.BooleanType;

    List<SymExpr> args = e.getArgs().stream()
            .map(SymExpr::fromJimple)
            .collect(Collectors.toList());

    return new SymInterfaceInvoke(base, invokedMethodName, returnsBoolean, args);
  }

  public SymInterfaceInvoke(
          final SymExpr base, final String signature, final boolean returnsBoolean,
          final List<SymExpr> args) {
    super(returnsBoolean ? SymKind.BOOLEAN_METHOD : SymKind.OTHER);
    this.base = base;
    this.signature = signature;
    this.returnsBoolean = returnsBoolean;
    this.args = args;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitInterfaceInvoke(this);
  }

  @Override
  public SymExpr substitute(final String invField, final SymExpr replacement) {
    SymExpr newBase = base.substitute(invField, replacement);
    List<SymExpr> newArgs = args.stream()
            .map(a -> a.substitute(invField, replacement))
            .toList();

    boolean baseChanged = newBase != base;
    boolean argsChanged = !IntStream.range(0, args.size())
            .allMatch(i -> args.get(i) == newArgs.get(i));

    if (baseChanged || argsChanged) {
      return new SymInterfaceInvoke(newBase, signature, returnsBoolean, newArgs);
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newBase = base.substituteParam(idx, actual);
    List<SymExpr> newArgs = args.stream()
            .map(a -> a.substituteParam(idx, actual))
            .toList();

    boolean baseChanged = newBase != base;
    boolean argsChanged = !IntStream.range(0, args.size())
            .allMatch(i -> args.get(i) == newArgs.get(i));

    if (baseChanged || argsChanged) {
      return new SymInterfaceInvoke(newBase, signature, returnsBoolean, newArgs);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName);
  }

  @Override
  public String toString() {
    String argStr = args.stream().map(SymExpr::toString).collect(Collectors.joining(","));
    return base.toString() + "." + signature + "(" + argStr + ")";
  }
}

