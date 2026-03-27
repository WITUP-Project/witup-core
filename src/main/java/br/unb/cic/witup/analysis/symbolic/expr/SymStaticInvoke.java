package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.List;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.types.PrimitiveType;

public final class SymStaticInvoke extends SymExpr {
  private final String invokeName; // e.g. length
  private final boolean returnsBoolean;
  private final List<SymExpr> args;
  private String cachedToString;

  public SymStaticInvoke(final JStaticInvokeExpr e) {
    this(e.getMethodSignature().toString(),
        deriveKind(e) == SymKind.BOOLEAN_METHOD,
        e.getArgs().stream().map(SymExpr::fromJimple).toList(),
        deriveKind(e));
  }

  private SymStaticInvoke(
      final String invokeName,
      final boolean returnsBoolean,
      final List<SymExpr> args,
      final SymKind kind) {
    super(kind, argsMask(args));
    this.invokeName = invokeName;
    this.returnsBoolean = returnsBoolean;
    this.args = args;
  }

  private static long argsMask(final List<SymExpr> args) {
    long mask = 0;
    for (SymExpr arg : args) {
      mask |= arg.getParamMask();
    }
    return mask;
  }

  public List<SymExpr> getArgs() {
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
    List<SymExpr> newArgs = null;
    for (int i = 0; i < args.size(); i++) {
      SymExpr newArg = args.get(i).substituteParam(idx, actual);
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
