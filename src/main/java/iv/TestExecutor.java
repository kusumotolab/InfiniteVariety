package iv;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iv.util.Timer;

public class TestExecutor {

  private static final Logger logger = LoggerFactory.getLogger(TestExecutor.class);
  private final IVConfig config;

  public TestExecutor(final IVConfig config) {
    logger.trace("enter TestExecutor(IVConfig)");
    this.config = config;
  }

  public static void main(final String[] args) {

    final ch.qos.logback.classic.Logger rootLog =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLog.setLevel(ch.qos.logback.classic.Level.ERROR);

    final IVConfig config = new IVConfig();
    final CmdLineParser cmdLineParser = new CmdLineParser(config);
    try {
      cmdLineParser.parseArgument(args);
    } catch (final CmdLineException e) {
      cmdLineParser.printUsage(System.err);
      return;
    }

    final Timer timer = new Timer();
    timer.start();

    final TestExecutor executor = new TestExecutor(config);
    executor.exec();

    timer.stop();
    logger.info("elapsed time: {}", timer);
  }

  public void exec() {
    logger.trace("enter exec()");

    // "-o" で指定されたディレクトリが存在しない場合にはエラー
    final Path outputPath = config.getOutputPath();
    if (!Files.exists(outputPath) || !Files.isDirectory(outputPath)) {
      System.err.println("specified path does not exist as a directory: " + outputPath);
      System.exit(0);
    }

    try {
      // 各カテゴリのディレクトリを得る
      final List<Path> groupDirs = Files.list(outputPath)
          .filter(Files::isDirectory)
          .collect(Collectors.toList());

      // 各カテゴリに対して処理を行う
      // ここでの処理とは，切り出したメソッドのコンパイルとテスト生成
      for (final Path groupDir : groupDirs) {
        final List<Path> targetDirs = Files.list(groupDir)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());

        final Map<Path, Path> sourceTestMap = new HashMap<>();

        // 各対象のクラス（メソッド）に対して処理を行う
        for (final Path targetDir : targetDirs) {

          // "_test"で終わる場合はテストディレクトリだからスキップ
          if (targetDir.toString()
              .endsWith("_test")) {
            continue;
          }

          // 対象のメソッドをコンパイル
          final List<String> javacCommand = new ArrayList<>();
          javacCommand.add("javac");
          javacCommand.add(targetDir.resolve("Target.java")
              .toString());
          final int javaCommandExitValue = executeProcess(javacCommand);

          // コンパイル失敗の場合はこのメソッドに対して以降の処理をしない
          if (javaCommandExitValue != 0) {
            continue;
          }

          // 対象のメソッドのテストを生成
          final Path testDir = Paths.get(targetDir + "_test");
          if (Files.notExists(testDir)) { // テストディレクトリが既に存在する場合は処理をスキップ
            final List<String> evosuiteCommand = new ArrayList<>();
            evosuiteCommand.add("java");
            evosuiteCommand.add("-jar");
            evosuiteCommand.add("lib/evosuite-1.1.0.jar");
            evosuiteCommand.add("-class");
            evosuiteCommand.add("Target");
            evosuiteCommand.add("-projectCP");
            evosuiteCommand.add(targetDir.toString());
            evosuiteCommand.add("-Dtest_dir=" + testDir);
            final int evosuiteCommandExitValue = executeProcess(evosuiteCommand);

            // evosuiteによるテスト生成が失敗した場合には以降の処理をしない
            if (evosuiteCommandExitValue != 0) {
              continue;
            }
          }

          // 生成したテストをコンパイル
          final List<String> javacCommand2 = new ArrayList<>();
          javacCommand2.add("javac");
          //javacCommand2.add(testDir.resolve("*.java").toString());
          javacCommand2.add(testDir.resolve("Target_ESTest.java")
              .toString());
          javacCommand2.add(testDir.resolve("Target_ESTest_scaffolding.java")
              .toString());
          final String cp = targetDir + ":" +
              testDir + ":" +
              "lib/evosuite-standalone-runtime-1.1.0.jar:" +
              "lib/junit-4.13.2.jar:" +
              "lib/hamcrest-core-1.3.jar";
          System.out.println("CLASSPATH=" + cp);
          final Map<String, String> environmentVariables1 = new HashMap<>();
          environmentVariables1.put("CLASSPATH", cp);
          final int javaCommandExitValue2 = executeProcess(javacCommand2, environmentVariables1);

          // コンパイルに失敗した場合には以降の処理をしない
          if (javaCommandExitValue2 != 0) {
            continue;
          }

          sourceTestMap.put(targetDir, testDir);
        }

        final Entry<Path, Path>[] entries = sourceTestMap.entrySet()
            .toArray(Entry[]::new);
        for (int left = 0; left < entries.length; left++) {
          for (int right = left + 1; right < entries.length; right++) {

            if (left == right) {
              continue;
            }

            final List<String> junit1command = new ArrayList<>();
            junit1command.add("java");
            junit1command.add("org.junit.runner.JUnitCore");
            junit1command.add("Target_ESTest");

            // leftのソースがrightのテストをパスするかを確認
            final String cp1 = entries[left].getKey() + ":" + entries[right].getValue() + ":" +
                "lib/evosuite-standalone-runtime-1.1.0.jar:" +
                "lib/junit-4.13.2.jar:" +
                "lib/hamcrest-core-1.3.jar";
            System.out.println(cp1);
            final Map<String, String> environmentVariables1 = new HashMap<>();
            environmentVariables1.put("CLASSPATH", cp1);
            final int junit1CommandExitValue = executeProcess(junit1command, environmentVariables1);
            if (junit1CommandExitValue != 0) {
              continue;
            }

            // rightのソースがleftのテストをパスするかを確認
            final String cp2 = entries[right].getKey() + ":" + entries[left].getValue() + ":" +
                "lib/evosuite-standalone-runtime-1.1.0.jar:" +
                "lib/junit-4.13.2.jar:" +
                "lib/hamcrest-core-1.3.jar";
            System.out.println(cp2);
            final Map<String, String> environmentVariables2 = new HashMap<>();
            environmentVariables2.put("CLASSPATH", cp2);
            final int junit2CommandExitValue = executeProcess(junit1command, environmentVariables2);
            if (junit2CommandExitValue != 0) {
              continue;
            }

            System.out.println(entries[left].getValue() + " : " + entries[right].getValue());
          }
        }

      }

      // テスト生成に成功した各メソッドの振る舞いが等価かを判定

    } catch (final IOException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  private int executeProcess(final List<String> command) {
    return executeProcess(command, Collections.EMPTY_MAP);
  }

  private int executeProcess(final List<String> command,
      final Map<String, String> environmentVariables) {
    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(command);
      processBuilder.redirectErrorStream(true);
      final Map<String, String> environment = processBuilder.environment();
      environment.put("JAVA_HOME",
          "/Library/Java/JavaVirtualMachines/jdk-15.0.2.jdk/Contents/Home");
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
        out.write(buf, 0, size);
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
}
