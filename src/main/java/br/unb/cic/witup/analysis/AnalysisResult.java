package br.unb.cic.witup.analysis;

import br.unb.cic.witup.analysis.graph.WITUpGraph;
import br.unb.cic.witup.solver.SolverResult;

import java.util.List;
import java.util.Map;

public record AnalysisResult(
        WITUpGraph graph,
        MethodSummary summary,
        List<SolverResult> solutions,
        Map<String, String> failures) {}
