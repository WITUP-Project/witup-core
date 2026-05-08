package br.unb.cic.witup.samples;

// Mirrors org.apache.commons.io.FileUtils#cleanDirectory + #verifiedListFiles, stripped to
// the bare callee/caller shape so the test exercises how `cleanDirectory` integrates
// `verifiedListFiles`'s throw-path conditions as preconditions for its own throw.
public class File {

  public static java.io.File[] verifiedListFiles(final java.io.File directory) {
    if (!directory.exists()) {
      throw new IllegalArgumentException();
    }
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException();
    }
    final java.io.File[] files = directory.listFiles();
    if (files == null) {
      throw new IllegalStateException();
    }
    return files;
  }

  // For this throw to fire, verifiedListFiles must have returned normally (so
  // exists() == true, isDirectory() == true, listFiles() != null) AND files.length == 0.
  public static void cleanDirectory(final java.io.File directory) {
    final java.io.File[] files = verifiedListFiles(directory);
    if (files.length == 0) {
      throw new IllegalArgumentException();
    }
  }
}
