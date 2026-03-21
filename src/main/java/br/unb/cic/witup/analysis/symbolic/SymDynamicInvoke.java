package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;

public final class SymDynamicInvoke extends SymExpr {
  private final String signature;
  private final List<SymExpr> args;

  public SymDynamicInvoke(final JDynamicInvokeExpr e) {
    super(SymKind.OTHER);
    this.signature = e.toString();
    this.args = e.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList());
  }

  private SymDynamicInvoke(final String signature, final List<SymExpr> args, final SymKind kind) {
    super(kind);
    this.signature = signature;
    this.args = args;
  }

  public String getSignature() {
    return signature;
  }

  public List<SymExpr> getArgs() {
    return args;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitDynamicInvoke(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    List<SymExpr> newArgs =
        args.stream().map(a -> a.substitute(varName, replacement)).collect(Collectors.toList());
    boolean changed = IntStream.range(0, args.size()).anyMatch(i -> args.get(i) != newArgs.get(i));
    return changed ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    List<SymExpr> newArgs =
        args.stream().map(a -> a.substituteParam(idx, actual)).collect(Collectors.toList());
    boolean changed = IntStream.range(0, args.size()).anyMatch(i -> args.get(i) != newArgs.get(i));
    return changed ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
  }

  @Override
  public String toString() {
    String argStr = args.stream().map(SymExpr::toString).collect(Collectors.joining(","));
    return "dynamicinvoke_" + signature + "(" + argStr + ")";
  }

  @Override
  public boolean contains(final String varName) {
    return args.stream().anyMatch(a -> a.contains(varName));
  }
}
