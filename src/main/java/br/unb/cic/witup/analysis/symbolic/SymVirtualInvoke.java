package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.types.PrimitiveType;

public final class SymVirtualInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String signature; // e.g. length
  private final boolean returnsBoolean;
  private final List<SymExpr> args;

  // we have access to getArgs, getArgCount, getMethodSignature, getType, getUses

  public SymExpr getBase() {
    return base;
  }

  public List<SymExpr> getArgs() {
    return args;
  }

  public static SymExpr fromVirtualInvokeExpr(final JVirtualInvokeExpr e) {
    SymExpr base = fromJimple(e.getBase());
    String invokedMethodName = e.getMethodSignature().getSubSignature().getName();
    boolean returnsBoolean =
        e.getMethodSignature().getSubSignature().getType() instanceof PrimitiveType.BooleanType;

    List<SymExpr> args = e.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());

    return new SymVirtualInvoke(base, invokedMethodName, returnsBoolean, args);
  }

  public SymVirtualInvoke(
      final SymExpr base,
      final String signature,
      final boolean returnsBoolean,
      final List<SymExpr> args) {
    super(returnsBoolean ? SymKind.BOOLEAN_METHOD : SymKind.OTHER);
    this.base = base;
    this.signature = signature;
    this.returnsBoolean = returnsBoolean;
    this.args = args;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitVirtualInvoke(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newBase = base.substitute(varName, replacement);
    List<SymExpr> newArgs = args.stream().map(a -> a.substitute(varName, replacement)).toList();

    boolean baseChanged = newBase != base;
    boolean argsChanged =
        !IntStream.range(0, args.size()).allMatch(i -> args.get(i) == newArgs.get(i));

    if (baseChanged || argsChanged) {
      return new SymVirtualInvoke(newBase, signature, returnsBoolean, newArgs);
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    SymExpr newBase = base.substituteParam(idx, actual);
    List<SymExpr> newArgs = args.stream().map(a -> a.substituteParam(idx, actual)).toList();

    boolean baseChanged = newBase != base;
    boolean argsChanged =
        !IntStream.range(0, args.size()).allMatch(i -> args.get(i) == newArgs.get(i));

    if (baseChanged || argsChanged) {
      return new SymVirtualInvoke(newBase, signature, returnsBoolean, newArgs);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName) || args.stream().anyMatch(a -> a.contains(varName));
  }

  @Override
  public String toString() {
    String argStr = args.stream().map(SymExpr::toString).collect(Collectors.joining(","));
    return base.toString() + "." + signature + "(" + argStr + ")";
  }
}
