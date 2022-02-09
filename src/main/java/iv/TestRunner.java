package iv;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class TestRunner {

  protected int executeProcess(final List<String> command) {
    return executeProcess(command, Collections.EMPTY_MAP);
  }

  protected int executeProcess(final List<String> command,
      final Map<String, String> environmentVariables) {
    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      final Map<String, String> environment = processBuilder.environment();
      environment.put("JAVA_HOME",
          "/Library/Java/JavaVirtualMachines/jdk-11.0.12.jdk/Contents/Home");
      environment.putAll(environmentVariables);
      final Process javacProcess = processBuilder.start();
      final StreamThread outThread = new StreamThread(javacProcess.getInputStream(), System.out);
      outThread.start();
      javacProcess.waitFor();
      outThread.join();
      return javacProcess.exitValue();
    } catch (final IOException | InterruptedException e) {
      e.printStackTrace();
      System.exit(0);
    }

    return 1;
  }

  protected String getClassPath(final Path targetDir, Path testDir) {
    return targetDir + ":" + testDir + ":" + "lib/evosuite-standalone-runtime-1.2.0.jar:" +
        "lib/junit-4.13.2.jar:" + "lib/hamcrest-core-1.3.jar";
  }

  class StreamThread extends Thread {

    private static final int BUF_SIZE = 4096;
    private final InputStream in;
    private final PrintStream out;

    public StreamThread(final InputStream in, final PrintStream out) throws IOException {
      this.in = in;
      this.out = out;
    }

    public void run() {
      byte[] buf = new byte[BUF_SIZE];
      int size = -1;
      try {
        while ((size = in.read(buf, 0, BUF_SIZE)) != -1) {
          //out.write(buf, 0, size); 出力うっとおしいのでコメントアウト
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }
}
