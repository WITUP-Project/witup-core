package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.core.types.PrimitiveType;

import java.util.List;
import java.util.stream.Collectors;

public final class SymStaticInvoke extends SymExpr {
  private final String invokeName; // e.g. length
  private final boolean returnsBoolean;
  private final List<SymExpr> args;

 public SymStaticInvoke(final JStaticInvokeExpr e) {
   super(deriveKind(e));
   this.invokeName = e.getMethodSignature().toString();
   this.returnsBoolean = getKind() == SymKind.BOOLEAN_METHOD;
   this.args = e.getArgs().stream().map(SymExpr::fromJimple).toList();
 }

 private SymStaticInvoke(
         final String invokeName, final boolean returnsBoolean,
         final List<SymExpr> args, final SymKind kind) {
   super(kind);
   this.invokeName = invokeName;
   this.returnsBoolean = returnsBoolean;
   this.args = args;
 }

 public List<SymExpr> getArgs() {
   return args;
 }

 public String getInvokeName() {
   return invokeName;
 }

 private static SymKind deriveKind(final JStaticInvokeExpr e) {
   return e.getMethodSignature().getSubSignature().getType() instanceof PrimitiveType.BooleanType
           ? SymKind.BOOLEAN_METHOD : SymKind.OTHER;
 }

   @Override
   public <T> T accept(final SymExprVisitor<T> visitor) {
     return visitor.visitStaticInvoke(this);
   }

   @Override
   public SymExpr substitute(final String varName, final SymExpr replacement) {
     List<SymExpr> newArgs = args.stream()
             .map(arg -> arg.substitute(varName, replacement))
             .toList();
     return new SymStaticInvoke(invokeName, returnsBoolean, newArgs, getKind());
   }

   @Override
   public SymExpr substituteParam(final int idx, final SymExpr actual) {
     List<SymExpr> newArgs = args.stream()
             .map(arg -> arg.substituteParam(idx, actual))
             .toList();
     return new SymStaticInvoke(invokeName, returnsBoolean, newArgs, getKind());
   }

   @Override
   public boolean contains(final String varName) {
     return args.stream().anyMatch(arg -> arg.contains(varName));
   }

  @Override
  public String toString() {
    String argStr = args.stream()
            .map(SymExpr::toString)
            .collect(Collectors.joining(","));
    return invokeName + "(" + argStr + ")";
  }
 }
