package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;

public final class SymDynamicInvoke extends SymExpr {
  private final String signature;
  private final List<SymExpr> args;
  private String cachedToString;

  public SymDynamicInvoke(final JDynamicInvokeExpr e) {
    this(e.toString(),
        e.getArgs().stream().map(SymExpr::fromJimple).collect(Collectors.toList()),
        SymKind.OTHER);
  }

  private SymDynamicInvoke(final String signature, final List<SymExpr> args, final SymKind kind) {
    super(kind, argsMask(args));
    this.signature = signature;
    this.args = args;
  }

  private static long argsMask(final List<SymExpr> args) {
    long mask = 0;
    for (SymExpr arg : args) {
      mask |= arg.getParamMask();
    }
    return mask;
  }

  public String getSignature() {
    return signature;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitDynamicInvoke(this);
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

    boolean argsChanged = false;
    for (int i = 0; i < args.size(); i++) {
      if (args.get(i) != newArgs.get(i)) {
        argsChanged = true;
        break;
      }
    }

    return argsChanged ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
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

    return newArgs != null ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      if (args.isEmpty()) {
        cachedToString = "dynamicinvoke_" + signature + "()";
      } else {
        StringBuilder sb = new StringBuilder("dynamicinvoke_");
        sb.append(signature).append("(");
        sb.append(args.get(0).toString());
        for (int i = 1; i < args.size(); i++) {
          sb.append(",").append(args.get(i).toString());
        }
        sb.append(")");
        cachedToString = sb.toString();
      }
    }
    return cachedToString;
  }

  @Override
  public boolean contains(final String varName) {
    return args.stream().anyMatch(a -> a.contains(varName));
  }
}
