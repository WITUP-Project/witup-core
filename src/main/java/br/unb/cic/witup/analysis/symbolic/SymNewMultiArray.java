package br.unb.cic.witup.analysis.symbolic;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class SymNewMultiArray extends SymExpr {
  private final String objectType;
  private final List<SymExpr> sizes;

  public SymNewMultiArray(final JNewMultiArrayExpr e) {
    super(SymKind.OTHER);
    this.objectType = e.getType().toString();
    this.sizes = e.getSizes().stream()
            .map(SymExpr::fromJimple)
            .collect(Collectors.toList());
  }

  private SymNewMultiArray(final String objectType, final List<SymExpr> sizes) {
    super(SymKind.OTHER);
    this.objectType = objectType;
    this.sizes = sizes;
  }

  public List<SymExpr> getSizes() {
    return sizes;
  }

  public String getObjectType() {
    return objectType;
  }

  @Override
  public <T> T accept(final SymExprVisitor<T> visitor) {
    return visitor.visitNewMultiArray(this);
  }

  @Override
  public SymExpr substitute(final String varName, final SymExpr replacement) {
    List<SymExpr> newSizes = sizes.stream()
            .map(s -> s.substitute(varName, replacement))
            .collect(Collectors.toList());
    if (!IntStream.range(0, sizes.size())
            .allMatch(i -> sizes.get(i) == newSizes.get(i))) {
      return new SymNewMultiArray(objectType, newSizes);
    }
    return this;
  }

  @Override
  public SymExpr substituteParam(final int idx, final SymExpr actual) {
    List<SymExpr> newSizes = sizes.stream()
            .map(s -> s.substituteParam(idx, actual))
            .collect(Collectors.toList());
    if (!IntStream.range(0, sizes.size())
            .allMatch(i -> sizes.get(i) == newSizes.get(i))) {
      return new SymNewMultiArray(objectType, newSizes);
    }
    return this;
  }

  @Override
  public boolean contains(final String varName) {
    return sizes.stream().anyMatch(s -> s.contains(varName));
  }

  @Override
  public String toString() {
    String dims = sizes.stream()
            .map(SymExpr::toString)
            .collect(Collectors.joining("][", "[", "]"));
    return "newmultiarray(" + objectType + ")" + dims;
  }
}
