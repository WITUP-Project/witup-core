package br.unb.cic.witup.analysis.loop;

import java.util.Map;

/**
 * The result of abstract interpretation over a single natural loop.
 *
 * @param loop the loop this summary describes
 * @param variableIntervals post-loop abstract interval for each variable modified in the loop
 * @param inductionVars recognised induction variables with their symbolic bounds
 * @param accumulations recognised accumulation patterns with array dependencies
 */
public record LoopSummary(
    NaturalLoop loop,
    Map<String, Interval> variableIntervals,
    Map<String, InductionInfo> inductionVars,
    Map<String, AccumulationInfo> accumulations) {}
