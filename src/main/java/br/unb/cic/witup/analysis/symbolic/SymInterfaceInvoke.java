package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JInterfaceInvokeExpr;
import sootup.core.types.PrimitiveType;

public final class SymInterfaceInvoke extends SymExpr {
  private final SymExpr base; // e.g. s
  private final String invokeName; // e.g. length
  private final boolean returnsBoolean;

  // we have access to getArgs, getArgCount, getMethodSignature, getType, getUses

  public SymExpr getBase() {
    return base;
  }

  public static SymExpr fromInterfaceInvokeExpr(final JInterfaceInvokeExpr e) {
    SymExpr base = fromJimple(e.getBase());
    String invokedMethodName = e.getMethodSignature().getSubSignature().getName();
    boolean returnsBoolean =
            e.getMethodSignature().getSubSignature().getType() instanceof PrimitiveType.BooleanType;

    return new SymSpecialInvoke(base, invokedMethodName, returnsBoolean);
  }

  public SymInterfaceInvoke(
          final SymExpr base, final String invokeName, final boolean returnsBoolean) {
    super(returnsBoolean ? SymKind.BOOLEAN_METHOD : SymKind.OTHER);
    this.base = base;
    this.invokeName = invokeName;
    this.returnsBoolean = returnsBoolean;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitInterfaceInvoke(this);
  }

  @Override
  public SymExpr substitute(final String invField, final SymExpr replacement) {
    SymExpr newBase = base.substitute(invField, replacement);
    if (newBase != base) {
      return new SymSpecialInvoke(newBase, invField, this.returnsBoolean);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return base.contains(varName);
  }

  @Override
  public String toString() {
    return base.toString() + "." + invokeName;
  }
}

