package br.unb.cic.witup.analysis.expath;

/**
 * Options to control exception-path (expath) extraction.
 *
 * <p>CFG path enumeration can blow up quickly in real-world code. These limits are meant to keep
 * the analysis practical by bounding the search space (in line with the paper's approach of using
 * heuristics/limits to favor precision and applicability).
 */
public record ExpathOptions(
        int maxPaths,
        int maxDepth,
        boolean useTrieCompression) {

    /**
     * Conservative defaults that work well for unit tests and medium-size projects.
     *
     * <p>Increase for higher recall if you can afford it.
     */
    public static final ExpathOptions DEFAULT = new ExpathOptions(20_000, 400, false);

    public ExpathOptions {
        if (maxPaths <= 0) {
            throw new IllegalArgumentException("maxPaths must be > 0");
        }
        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth must be > 0");
        }
    }
}
