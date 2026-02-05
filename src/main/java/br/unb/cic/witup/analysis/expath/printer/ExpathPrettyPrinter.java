package br.unb.cic.witup.analysis.expath.printer;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Pretty printer for local/global expaths visualization.
 */
public final class ExpathPrettyPrinter {

    public String printLocalExpaths(
            String methodSignature,
            List<List<ResolvedThrowCondition>> localExpaths
    ) {
        Objects.requireNonNull(methodSignature, "methodSignature");
        Objects.requireNonNull(localExpaths, "localExpaths");

        StringBuilder sb = new StringBuilder();
        sb.append("=== LOCAL EXPATHS ===\n");
        sb.append("method: ").append(methodSignature).append('\n');
        appendResolvedPaths(sb, localExpaths);
        return sb.toString();
    }

    public String printGlobalExpathsBeforeBinding(
            String rootSignature,
            int depth,
            List<List<ResolvedThrowCondition>> globalExpaths
    ) {
        Objects.requireNonNull(rootSignature, "rootSignature");
        Objects.requireNonNull(globalExpaths, "globalExpaths");

        StringBuilder sb = new StringBuilder();
        sb.append("=== GLOBAL EXPATHS (BEFORE BINDING) ===\n");
        sb.append("root: ").append(rootSignature).append('\n');
        sb.append("depth: ").append(depth).append('\n');
        appendResolvedPaths(sb, globalExpaths);
        return sb.toString();
    }

    public String printGlobalExpathsAfterBinding(
            String rootSignature,
            int depth,
            List<List<ResolvedThrowCondition>> boundGlobalExpaths
    ) {
        Objects.requireNonNull(rootSignature, "rootSignature");
        Objects.requireNonNull(boundGlobalExpaths, "boundGlobalExpaths");

        StringBuilder sb = new StringBuilder();
        sb.append("=== GLOBAL EXPATHS (AFTER BINDING) ===\n");
        sb.append("root: ").append(rootSignature).append('\n');
        sb.append("depth: ").append(depth).append('\n');
        appendResolvedPaths(sb, boundGlobalExpaths);
        return sb.toString();
    }

    private void appendResolvedPaths(StringBuilder sb, List<List<ResolvedThrowCondition>> paths) {
        if (paths.isEmpty()) {
            sb.append("(empty)\n");
            return;
        }

        for (int i = 0; i < paths.size(); i++) {
            List<ResolvedThrowCondition> path = paths.get(i);
            sb.append("G").append(i + 1).append(" [size=").append(path.size()).append("]: ");
            sb.append(formatResolvedPath(path)).append('\n');
        }
    }

    private String formatResolvedPath(List<ResolvedThrowCondition> path) {
        StringJoiner joiner = new StringJoiner(" -> ", "[", "]");
        for (ResolvedThrowCondition c : path) {
            joiner.add(formatCondition(String.valueOf(c.getNode()), c.isTruthValue()));
        }
        return joiner.toString();
    }

    private String formatCondition(String expr, boolean truth) {
        return "(" + expr + " == " + truth + ")";
    }
}

