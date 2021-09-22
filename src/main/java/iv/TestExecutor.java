package iv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iv.db.JavaMethodDAO;
import iv.util.Timer;

public class TestExecutor extends TestRunner {

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

    // 指定されたデータベースを読み込む
    JavaMethodDAO.SINGLETON.initialize(config);

    try {
      // 各カテゴリのディレクトリを得る
      final List<Path> groupDirs = Files.list(outputPath)
          .filter(Files::isDirectory)
          .collect(Collectors.toList());

      // 各カテゴリに対する処理のループ
      for (final Path groupDir : groupDirs) {
        final List<Path> targetDirs = Files.list(groupDir)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());

        // テストが生成されているディレクトリ（メソッド）のみを取得
        final List<Path> compilableMethodDirs = targetDirs.stream()
            .filter(t -> JavaMethodDAO.SINGLETON.isCompilable(t.getFileName()
                .toString()))
            .collect(
                Collectors.toList());

        // メソッドAを，メソッドBのテストケースを利用してテストする．
        for (int leftIndex = 0; leftIndex < compilableMethodDirs.size(); leftIndex++) {

          // leftのソースディレクトリおよびテストディレクトリを取得
          final Path leftSourceDir = compilableMethodDirs.get(leftIndex);
          final String leftSourceDirName = leftSourceDir.getFileName()
              .toString();
          final String leftTestDirName = leftSourceDirName + "_test";
          final Path leftTestDir = leftSourceDir.resolveSibling(leftTestDirName);

          for (int rightIndex = leftIndex + 1; rightIndex < compilableMethodDirs.size();
              rightIndex++) {

            if (leftIndex == rightIndex) {
              continue;
            }

            // rightのソースディレクトリおよびテストディレクトリを取得
            final Path rightSourceDir = compilableMethodDirs.get(rightIndex);
            final String rightSourceDirName = rightSourceDir.getFileName()
                .toString();
            final String rightTestDirName = rightSourceDirName + "_test";
            final Path rightTestDir = leftSourceDir.resolveSibling(rightTestDirName);

            System.out.println("left source: " + leftSourceDir);
            System.out.println("left test: " + leftTestDir);
            System.out.println("right source: " + rightSourceDir);
            System.out.println("right test: " + rightTestDir);

            final List<String> command = Arrays.asList("java",
                "org.junit.runner.JUnitCore",
                "Target_ESTest");

            // leftのソースがrightのテストをパスするかを確認
            final String classpath1 = getClassPath(leftSourceDir, rightTestDir);
            final Map<String, String> environmentVariables1 = new HashMap<>();
            environmentVariables1.put("CLASSPATH", classpath1);
            final int junit1CommandExitValue = executeProcess(command, environmentVariables1);
            if (junit1CommandExitValue != 0) {
              continue;
            }

            // rightのソースがleftのテストをパスするかを確認
            final String classpath2 = getClassPath(rightSourceDir, leftTestDir);
            final Map<String, String> environmentVariables2 = new HashMap<>();
            environmentVariables2.put("CLASSPATH", classpath2);
            final int junit2CommandExitValue = executeProcess(command, environmentVariables2);
            if (junit2CommandExitValue != 0) {
              continue;
            }

            System.out.println("@@@@@Same functionality: " + leftSourceDirName + " : " + rightSourceDirName);
          }
        }
      }


    } catch (final IOException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }
}
