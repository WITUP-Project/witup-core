package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.common.expr.JVirtualInvokeExpr;
import sootup.core.types.PrimitiveType;

public final class SymVirtualInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String signature; // e.g. length
  private final boolean returnsBoolean;
  private final SymExpr[] args;
  private final boolean hasUnboxing;
  private String cachedToString;

  public SymExpr getBase() {
    return base;
  }

  public SymExpr[] getArgs() {
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

    SymExpr[] args = e.getArgs().stream().map(SymExpr::fromJimple).toArray(SymExpr[]::new);

    return new SymVirtualInvoke(base, invokedMethodName, returnsBoolean, args);
  }

  public SymVirtualInvoke(
      final SymExpr base,
      final String signature,
      final boolean returnsBoolean,
      final SymExpr[] args) {
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

    SymExpr[] newArgs = null;
    for (int i = 0; i < args.length; i++) {
      SymExpr newArg = args[i].substitute(varName, replacement);
      if (newArg != args[i]) {
        if (newArgs == null) {
          newArgs = args.clone();
        }
        newArgs[i] = newArg;
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

    SymExpr[] newArgs = null;
    for (int i = 0; i < args.length; i++) {
      SymExpr newArg = args[i].substituteParam(idx, actual);
      if (newArg != args[i]) {
        if (newArgs == null) {
          newArgs = args.clone();
        }
        newArgs[i] = newArg;
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

  private static long baseArgsMask(final SymExpr base, final SymExpr[] args) {
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
  public void collectVarNames(final Set<String> vars) {
    base.collectVarNames(vars);
    for (SymExpr arg : args) {
      arg.collectVarNames(vars);
    }
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    SymExpr newBase = base.resolveWith(env);
    SymExpr[] newArgs = null;
    for (int i = 0; i < args.length; i++) {
      SymExpr newArg = args[i].resolveWith(env);
      if (newArg != args[i]) {
        if (newArgs == null) {
          newArgs = args.clone();
        }
        newArgs[i] = newArg;
      }
    }
    return (newBase != base || newArgs != null)
        ? new SymVirtualInvoke(newBase, signature, returnsBoolean, newArgs != null ? newArgs : args)
        : this;
  }

  @Override
  public String toString() {
    if (cachedToString != null) {
      return cachedToString;
    }
    StringBuilder sb = new StringBuilder(base.toString());
    sb.append(".").append(signature).append("(");
    if (args.length > 0) {
      sb.append(args[0].toString());
      for (int i = 1; i < args.length; i++) {
        sb.append(",").append(args[i].toString());
      }
    }
    sb.append(")");
    cachedToString = sb.toString();
    return cachedToString;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymVirtualInvoke symVirtualInvoke)) {
      return false;
    }
    return returnsBoolean == symVirtualInvoke.returnsBoolean
        && signature.equals(symVirtualInvoke.signature)
        && base.equals(symVirtualInvoke.base)
        && Arrays.equals(args, symVirtualInvoke.args);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime
            * (prime * (prime * signature.hashCode() + base.hashCode()) + Arrays.hashCode(args))
        + Boolean.hashCode(returnsBoolean);
  }
}
