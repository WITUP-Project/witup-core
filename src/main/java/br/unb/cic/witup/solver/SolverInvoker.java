package br.unb.cic.witup.solver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class SolverInvoker {

  private static final String PYTHON_ENV_PATH = "PYTHON_ENV_PATH";
  private final String pythonScriptPath;

  public SolverInvoker(final String pythonScriptPath) {
    this.pythonScriptPath = pythonScriptPath;
  }

  /**
   * Calls a Python solver script with the given JSON request. Python logs go to JVM stderr; the
   * JSON response is returned as a string.
   */
  public String callSolver(final JSONObject request) throws IOException, InterruptedException {
    ProcessBuilder pb =
        new ProcessBuilder(System.getenv(PYTHON_ENV_PATH), pythonScriptPath);
    // redirect stderr
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);

    Process process = pb.start();

    try (OutputStream os = process.getOutputStream()) {
      os.write(request.toString().getBytes(StandardCharsets.UTF_8));
      os.flush();
    }

    StringBuilder stdout = new StringBuilder();
    try (BufferedReader br =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        stdout.append(line).append("\n");
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Python solver exited with code " + exitCode);
    }

    return stdout.toString().trim();
  }
}
