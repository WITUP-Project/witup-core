package br.unb.cic.witup.analysis.symbolic.expr;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import sootup.core.types.PrimitiveType;

/**
 * The paramMask invariant every compound node has to honour.
 *
 * <p>Compound nodes short-circuit substitution on the mask — `SymBinOp.substituteParam` opens with
 * `if (!containsParam(idx)) return this;`. A node that holds children but does not OR their masks
 * into its own therefore makes any parameter beneath it invisible, and the callee's predicate
 * silently arrives at the caller unsubstituted. The node's own substituteParam can be perfectly
 * correct and never be reached.
 *
 * <p>Only nodes constructible without Jimple values are covered here. SymFieldAccess, SymCast,
 * SymInstanceOf, SymNeg, SymArray and SymNewMultiArray need JCastExpr/JInstanceFieldRef/etc., so
 * they are exercised end to end instead.
 */
public class SymExprParamMaskTest {

  private static final SymParamRef PARAM = new SymParamRef(0, PrimitiveType.getInt());
  private static final SymExpr ACTUAL = SymVar.fresh("callerLocal", SymKind.INT);

  static Stream<Arguments> wrappers() {
    return Stream.of(
        args("SymLength", SymLength::new),
        args("SymArrayRef(array)", p -> new SymArrayRef(p, SymIntConst.of(0))),
        args("SymArrayRef(index)", p -> new SymArrayRef(SymVar.fresh("arr", SymKind.INT), p)),
        args("SymBinOp", p -> new SymBinOp(BinOp.LT, p, SymIntConst.of(1))),
        args("SymITE", p -> new SymITE(SymIntConst.one(), p, SymIntConst.zero())),
        // The shape that actually shows up: a wrapped parameter compared against something, so
        // the outer comparison is what the walker calls substituteParam on.
        args(
            "SymBinOp(SymLength(param))",
            p -> new SymBinOp(BinOp.EQ, new SymLength(p), SymIntConst.zero())),
        args(
            "SymBinOp(SymArrayRef(param))",
            p ->
                new SymBinOp(BinOp.LT, new SymArrayRef(p, SymIntConst.of(0)), SymIntConst.zero())));
  }

  private static Arguments args(final String name, final UnaryOperator<SymExpr> wrap) {
    return Arguments.of(Named.of(name, wrap));
  }

  @ParameterizedTest(name = "{0} propagates the parameter mask")
  @MethodSource("wrappers")
  public void wrappingAParameterKeepsItVisible(final UnaryOperator<SymExpr> wrap) {
    SymExpr wrapped = wrap.apply(PARAM);
    assertTrue(
        wrapped.containsParam(0),
        "a node holding a parameter must report it, or compound parents skip substitution: "
            + wrapped);
  }

  @ParameterizedTest(name = "{0} substitutes the parameter")
  @MethodSource("wrappers")
  public void wrappingAParameterStillSubstitutes(final UnaryOperator<SymExpr> wrap) {
    SymExpr wrapped = wrap.apply(PARAM);
    SymExpr substituted = wrapped.substituteParam(0, ACTUAL);
    assertNotEquals(
        wrapped, substituted, "substituting @parameter0 must rewrite the expression: " + wrapped);
    assertTrue(
        substituted.toString().contains("callerLocal"),
        "the caller's actual must appear in the result, got " + substituted);
  }
}
