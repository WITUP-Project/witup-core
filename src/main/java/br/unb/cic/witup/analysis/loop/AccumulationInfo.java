package br.unb.cic.witup.analysis.loop;

import br.unb.cic.witup.analysis.symbolic.expr.BinOp;
import br.unb.cic.witup.analysis.symbolic.expr.SymExpr;

/**
 * Describes an accumulation pattern in a loop (e.g. {@code sum = sum + arr[i]}).
 *
 * @param varName the accumulated variable name
 * @param operation the binary operation (ADD, SUB, MUL)
 * @param stepExpr the resolved non-self operand (e.g. {@code SymArrayRef(arr, i)})
 * @param initExpr the pre-loop initial value expression
 * @param inductionVar the induction variable name referenced in stepExpr, or null
 */
public record AccumulationInfo(
    String varName, BinOp operation, SymExpr stepExpr, SymExpr initExpr, String inductionVar) {}
