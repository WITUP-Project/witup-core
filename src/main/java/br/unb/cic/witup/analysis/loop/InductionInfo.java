package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;

/**
 * Describes a recognised induction variable in a loop (e.g. {@code i = 0; i < arr.length; i++}).
 *
 * @param varName the local variable name
 * @param initExpr the expression that initialises the variable before the loop
 * @param boundExpr the loop bound expression (e.g. arr.length)
 * @param step the constant increment per iteration (typically 1)
 * @param comparison the relational operator from the loop exit condition (e.g. LT for {@code <})
 */
public record InductionInfo(
    String varName, SymExpr initExpr, SymExpr boundExpr, long step, BinOp comparison) {}
