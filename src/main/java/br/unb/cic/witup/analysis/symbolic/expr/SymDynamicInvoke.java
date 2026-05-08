package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.common.expr.JDynamicInvokeExpr;

public final class SymDynamicInvoke extends SymExpr {
  private final String signature;
  private final SymExpr[] args;
  private String cachedToString;

  public SymDynamicInvoke(final JDynamicInvokeExpr e) {
    this(
        e.toString(),
        e.getArgs().stream().map(SymExpr::fromJimple).toArray(SymExpr[]::new),
        SymKind.OTHER);
  }

  private SymDynamicInvoke(final String signature, final SymExpr[] args, final SymKind kind) {
    super(kind, argsMask(args));
    this.signature = signature;
    this.args = args;
  }

  private static long argsMask(final SymExpr[] args) {
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

    return newArgs != null ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
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

    return newArgs != null ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      if (args.length == 0) {
        cachedToString = "dynamicinvoke_" + signature + "()";
      } else {
        StringBuilder sb = new StringBuilder("dynamicinvoke_");
        sb.append(signature).append("(");
        sb.append(args[0].toString());
        for (int i = 1; i < args.length; i++) {
          sb.append(",").append(args[i].toString());
        }
        sb.append(")");
        cachedToString = sb.toString();
      }
    }
    return cachedToString;
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
  public void collectVarNames(final Set<String> vars) {
    for (SymExpr arg : args) {
      arg.collectVarNames(vars);
    }
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
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
    return newArgs != null ? new SymDynamicInvoke(signature, newArgs, getKind()) : this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymDynamicInvoke symDynamicInvoke)) {
      return false;
    }
    return signature.equals(symDynamicInvoke.signature)
        && Arrays.equals(args, symDynamicInvoke.args);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    return prime * signature.hashCode() + Arrays.hashCode(args);
  }

  public SymExpr[] getArgs() {
    return args;
  }
}
