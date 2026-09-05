package br.unb.cic.witup.analysis.symbolic.expr;

import br.unb.cic.witup.analysis.symbolic.SymExprVisitor;
import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;

public final class SymNewMultiArray extends SymExpr {
  private final String objectType;
  private final List<SymExpr> sizes;
  private String cachedToString;
  private int cachedHashCode;

  public SymNewMultiArray(final JNewMultiArrayExpr e) {
    this(
        e.getType().toString(),
        e.getSizes().stream().map(SymExpr::fromJimple).collect(Collectors.toList()));
  }

  private SymNewMultiArray(final String objectType, final List<SymExpr> sizes) {
    super(SymKind.OTHER, maskOf(sizes));
    this.objectType = objectType;
    this.sizes = sizes;
  }

  public List<SymExpr> getSizes() {
    return sizes;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitNewMultiArray(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    List<SymExpr> newSizes = null;
    for (int i = 0; i < sizes.size(); i++) {
      SymExpr newArg = sizes.get(i).substitute(varName, replacement);
      if (newArg != sizes.get(i) && newSizes == null) {
        newSizes = new ArrayList<>(sizes.size());
        for (int j = 0; j < i; j++) {
          newSizes.add(sizes.get(j));
        }
      }
      if (newSizes != null) {
        newSizes.add(newArg);
      }
    }

    boolean sizesChanged = false;
    for (int i = 0; i < sizes.size(); i++) {
      if (sizes.get(i) != newSizes.get(i)) {
        sizesChanged = true;
        break;
      }
    }

    if (sizesChanged) {
      return new SymNewMultiArray(objectType, newSizes);
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    List<SymExpr> newSizes =
        sizes.stream().map(s -> s.substituteParam(idx, actual)).collect(Collectors.toList());

    boolean sizesChanged = false;
    for (int i = 0; i < sizes.size(); i++) {
      if (sizes.get(i) != newSizes.get(i)) {
        sizesChanged = true;
        break;
      }
    }

    if (sizesChanged) {
      return new SymNewMultiArray(objectType, newSizes);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return sizes.stream().anyMatch(s -> s.contains(varName));
  }

  @Override
  public void collectVarNames(final Set<String> vars) {
    for (SymExpr size : sizes) {
      size.collectVarNames(vars);
    }
  }

  @Override
  public SymExpr resolveWith(final Map<String, SymExpr> env) {
    List<SymExpr> newSizes = null;
    for (int i = 0; i < sizes.size(); i++) {
      SymExpr newSize = sizes.get(i).resolveWith(env);
      if (newSize != sizes.get(i)) {
        if (newSizes == null) {
          newSizes = new ArrayList<>(sizes);
        }
        newSizes.set(i, newSize);
      }
    }
    return newSizes != null ? new SymNewMultiArray(objectType, newSizes) : this;
  }

  @Override
  public String toString() {
    if (cachedToString == null) {
      StringBuilder sb = new StringBuilder("newmultiarray(");
      sb.append(objectType).append(")");
      for (SymExpr size : sizes) {
        sb.append("[").append(size.toString()).append("]");
      }
      cachedToString = sb.toString();
    }
    return cachedToString;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SymNewMultiArray symNewMultiArray)) {
      return false;
    }
    return objectType.equals(symNewMultiArray.objectType) && sizes.equals(symNewMultiArray.sizes);
  }

  @Override
  public int hashCode() {
    int h = cachedHashCode;
    if (h == 0) {
      final int prime = 31;
      h = prime * objectType.hashCode() + sizes.hashCode();
      cachedHashCode = h;
    }
    return h;
  }
}
