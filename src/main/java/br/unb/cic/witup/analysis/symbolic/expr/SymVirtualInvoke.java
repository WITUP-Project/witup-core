package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.types.PrimitiveType;

public final class SymVirtualInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String signature; // e.g. length
  private final boolean returnsBoolean;
  private final List<SymExpr> args;
  private final boolean hasUnboxing;
  private String cachedToString;

  public SymExpr getBase() {
    return base;
  }

  public List<SymExpr> getArgs() {
    return args;
  }

  public String getSignature() {
    return signature;
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
    super(returnsBoolean ? SymKind.BOOLEAN_METHOD : SymKind.OTHER, baseArgsMask(base, args));
    this.base = base;
    this.signature = signature;
    this.returnsBoolean = returnsBoolean;
    this.args = args;
    this.hasUnboxing = SymExpr.isUnboxingCall(signature) || base.containsUnboxing();
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitVirtualInvoke(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    SymExpr newBase = base.substitute(varName, replacement);

    List<SymExpr> newArgs = null;
    for (int i = 0; i < args.size(); i++) {
      SymExpr newArg = args.get(i).substitute(varName, replacement);
      if (newArg != args.get(i) && newArgs == null) {
        newArgs = new ArrayList<>(args.size());
        for (int j = 0; j < i; j++) {
          newArgs.add(args.get(j));
        }
      }
      if (newArgs != null) {
        newArgs.add(newArg);
      }
    }

    if (newBase != base || newArgs != null) {
      return new SymVirtualInvoke(
          newBase, signature, returnsBoolean, newArgs != null ? newArgs : args);
    }
    return this;
  }

  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    if (!containsParam(idx)) {
      return this;
    }
    SymExpr newBase = base.substituteParam(idx, actual);

    List<SymExpr> newArgs = null;
    for (int i = 0; i < args.size(); i++) {
      SymExpr newArg = args.get(i).substituteParam(idx, actual);
      if (newArg != args.get(i) && newArgs == null) {
        // first change — copy up to this point
        newArgs = new ArrayList<>(args.size());
        for (int j = 0; j < i; j++) {
          newArgs.add(args.get(j));
        }
      }
      if (newArgs != null) {
        newArgs.add(newArg);
      }
    }

    if (newBase != base || newArgs != null) {
      return new SymVirtualInvoke(
          newBase, signature, returnsBoolean, newArgs != null ? newArgs : args);
    }
    return this;
  }

  @Override
  public boolean containsUnboxing() {
    return hasUnboxing;
  }

  private static long baseArgsMask(final SymExpr base, final List<SymExpr> args) {
    long mask = base.getParamMask();
    for (SymExpr arg : args) {
      mask |= arg.getParamMask();
    }
    return mask;
  }

  @Override
  public boolean contains(final String varName) {
    if (base.contains(varName)) {
      return true;
    }
    for (SymExpr arg : args) {
      if (arg.contains(varName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    if (cachedToString != null) {
      return cachedToString;
    }
    StringBuilder sb = new StringBuilder(base.toString());
    sb.append(".").append(signature).append("(");
    if (!args.isEmpty()) {
      sb.append(args.get(0).toString());
      for (int i = 1; i < args.size(); i++) {
        sb.append(",").append(args.get(i).toString());
      }
    }
    sb.append(")");
    cachedToString = sb.toString();
    return cachedToString;
  }
}
