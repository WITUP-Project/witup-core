package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.Map;
import java.util.Set;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;

public final class SymVar extends SymExpr {
  public static final String VAR_PREFIX = "var_";
  public static final String STACK_PREFIX = "stack_";
  private static final String STACK_LOCAL_PREFIX = "$stack";

  private final String name;
  private final String typeName;

  public SymVar(final Local l) {
    super(fromJimpleType(l.getType()));
    this.name = normalise(l.getName());
    this.typeName = l.getType().toString();
  }

  // Canonical name for any Jimple Value that flows through SymExpr — used wherever
  // the constraint pipeline needs to compare a Jimple-side string against a SymVar's
  // stored name. Anything that is not a Local falls through to toString() since other
  // Jimple Values (refs, immediates) do not participate in the local-rename scheme.
  public static String nameOf(final Value v) {
    if (v instanceof Local l) {
      return normalise(l.getName());
    }
    return v.toString();
  }

  // SootUp default-named locals (l0, l1, ...) and stack temps ($stack3) are not
  // user-meaningful — surface them as var_N / stack_N so model output and constraint
  // dumps don't expose internal Jimple naming. User-named locals pass through.
  // Open-coded char checks instead of Pattern.matcher() because this is on the hot
  // SymExpr-construction path and Matcher allocations dominated young-gen pressure.
  private static String normalise(final String name) {
    if (name.length() >= 2 && name.charAt(0) == 'l' && allDigits(name, 1)) {
      return VAR_PREFIX + name.substring(1);
    }
    if (name.startsWith(STACK_LOCAL_PREFIX)
        && name.length() > STACK_LOCAL_PREFIX.length()
        && allDigits(name, STACK_LOCAL_PREFIX.length())) {
      return STACK_PREFIX + name.substring(STACK_LOCAL_PREFIX.length());
    }
    return name;
  }

  private static boolean allDigits(final String s, final int start) {
    for (int i = start; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  private SymVar(final String name, final SymKind kind) {
    super(kind);
    this.name = name;
    this.typeName = kind.toString();
  }

  public static SymVar fresh(final String name, final SymKind kind) {
    return new SymVar(name, kind);
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitVar(this);
  }

  public String getName() {
    return name;
  }

  public String getTypeName() {
    return typeName;
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    if (this.name.equals(varName)) {
      return replacement;
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return this.name.equals(varName);
  }

  @Override
  public void collectVarNames(final Set<String> vars) {
    vars.add(name);
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    SymExpr replacement = env.remove(name);
    if (replacement == null) {
      return this;
    }
    SymExpr result = replacement.resolveWith(env);
    env.put(name, replacement);
    return result;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymVar symVar)) {
      return false;
    }
    return name.equals(symVar.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }
}
