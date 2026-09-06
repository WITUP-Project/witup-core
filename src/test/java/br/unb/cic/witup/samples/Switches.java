package br.unb.cic.witup.samples;

public class Switches {
  // Shape of FileSystemUtils.freeSpaceOS: a guard, then a switch whose arms throw. The guard
  // matters — it puts an IfStatementNode on every path, which is what getThrowPaths requires
  // before it will keep a path at all. The switch arms are reachable only through switch edges,
  // so nothing downstream of `switch` is visible unless those edges are CFG edges.
  public static int classify(String name, int kind) {
    if (name == null) {
      throw new IllegalArgumentException("name must not be null");
    }
    switch (kind) {
      case 0:
        return 10;
      case 1:
        throw new IllegalStateException("unsupported kind");
      default:
        throw new UnsupportedOperationException("unknown kind");
    }
  }
}
