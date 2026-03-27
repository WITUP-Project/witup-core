package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.types.PrimitiveType;

public final class SymStaticInvoke extends SymExpr {
  private final String invokeName; // e.g. length
  private final boolean returnsBoolean;
  private final SymExpr[] args;
  private String cachedToString;

  public SymStaticInvoke(final JStaticInvokeExpr e) {
    this(
        e.getMethodSignature().toString(),
        deriveKind(e) == SymKind.BOOLEAN_METHOD,
        e.getArgs().stream().map(SymExpr::fromJimple).toArray(SymExpr[]::new),
        deriveKind(e));
  }

  private SymStaticInvoke(
      final String invokeName,
      final boolean returnsBoolean,
      final SymExpr[] args,
      final SymKind kind) {
    super(kind, argsMask(args));
    this.invokeName = invokeName;
    this.returnsBoolean = returnsBoolean;
    this.args = args;
  }

  private static long argsMask(final SymExpr[] args) {
    long mask = 0;
    for (SymExpr arg : args) {
      mask |= arg.getParamMask();
    }
    return mask;
  }

  public SymExpr[] getArgs() {
    return args;
  }

  public String getInvokeName() {
    return invokeName;
  }

  private static SymKind deriveKind(final JStaticInvokeExpr e) {
    return e.getMethodSignature().getSubSignature().getType() instanceof PrimitiveType.BooleanType
        ? SymKind.BOOLEAN_METHOD
        : SymKind.OTHER;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitStaticInvoke(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
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
    if (newArgs != null) {
      return new SymStaticInvoke(invokeName, returnsBoolean, newArgs, getKind());
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    if (!containsParam(idx)) {
      return this;
    }
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
    if (newArgs != null) {
      return new SymStaticInvoke(invokeName, returnsBoolean, newArgs, getKind());
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
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
    StringBuilder sb = new StringBuilder(invokeName).append("(");
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
}
