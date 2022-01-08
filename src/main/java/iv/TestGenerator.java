package iv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iv.db.JavaMethodDAO;
import iv.util.Timer;

public class TestGenerator extends TestRunner {

  private static final Logger logger = LoggerFactory.getLogger(TestGenerator.class);
  private final IVConfig config;

  public TestGenerator(final IVConfig config) {
    logger.trace("enter TestGenerator(IVConfig)");
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

    final TestGenerator generator = new TestGenerator(config);
    generator.exec();

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

      final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
          .availableProcessors());

      // 各カテゴリに対する処理のループ
      // TODO このループを，指定された下限から上限の範囲のグループへのループに変更
      // IVConfigに下限と条件を指定するためのオプションを指定する必要あり
      final int lowerBound = config.getLowerBound();
      System.out.println("target lower bound is set to " + lowerBound);
      final int upperBound = config.getUpperBound();
      System.out.println("target upper bound is set to " + upperBound);
      for (final Path groupDir : groupDirs) {

        // TODO 対象のメソッドグループ以外の処理をしないように変更．
        // TODO 実装はしたが，テストはまだ．
        final String groupDirName = groupDir.getFileName()
            .toString();
        if (!groupDirName.contains(".")) {
          System.out.println(groupDirName + " is not a directory for method group");
          continue;
        }
        final int groupID = Integer.valueOf(groupDirName.substring(0, groupDirName.indexOf('.')));
        if (groupID < lowerBound || upperBound < groupID) {
          continue;
        }

        final List<Path> targetDirs = Files.list(groupDir)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());

        // 各対象クラス（メソッド）に対する処理のループ
        final AtomicInteger targetIndexGenerator = new AtomicInteger(1);
        for (final Path targetDir : targetDirs) {
          final int targetIndex = targetIndexGenerator.getAndIncrement();

          // ディレクトリ名がメソッドIDでない場合は対象メソッドではないからスキップ
          final String targetDirName = targetDir.getFileName()
              .toString();
          if (!JavaMethodDAO.SINGLETON.exists(targetDirName)) {
            continue;
          }

          //System.out.println("trying to generate tests for " + targetDirName);
          executorService.execute(new Runnable() {

            @Override
            public void run() {
              {// 対象のメソッドをコンパイル
                final List<String> command = Arrays.asList("javac",
                    targetDir.resolve("Target.java")
                        .toString());
                final int exitValue = executeProcess(command);

                // コンパイルの可否をデータベースに保存
                final int methodID = Integer.valueOf(targetDirName);
                final boolean compilable = 0 == exitValue;
                JavaMethodDAO.SINGLETON.setCompilable(methodID, compilable);

                // コンパイル失敗の場合はこのメソッドに対して以降の処理をしない
                if (exitValue != 0) {
                  return;
                }
              }

              final LocalDateTime localDate = LocalDateTime.now();
              final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                  "yyyy/MM/dd HH:mm:ss");
              final String formattedTime = localDate.format(formatter);
              final StringBuilder text = new StringBuilder();
              text.append(formattedTime);
              text.append(" [group ");
              text.append(groupID);
              text.append("][");
              text.append(targetIndex);
              text.append("/");
              text.append(targetDirs.size());
              text.append("] generating test cases for ");
              text.append(groupDir.getFileName());
              System.out.println(text.toString());

              // 対象のメソッドのテストを生成
              final Path testDir = Paths.get(targetDir + "_test");

              {
                if (Files.notExists(testDir)) { // テストディレクトリが既に存在する場合は処理をスキップ
                  final List<String> command = Arrays.asList("java", "-jar",
                      "lib/evosuite-1.2.0.jar", "-class", "Target", "-projectCP",
                      targetDir.toString(),
                      "-Dtest_dir=" + testDir);
                  final int exitValue = executeProcess(command);

                  // evosuiteによるテスト生成が失敗した場合には以降の処理をしない
                  if (exitValue != 0) {
                    return;
                  }
                }
              }

              {// 生成したテストをコンパイル
                final Path target_ESTest = testDir.resolve("Target_ESTest.java");
                final Path target_ESTest_scaffolding = testDir.resolve(
                    "Target_ESTest_scaffolding.java");
                final List<String> command = Arrays.asList("javac",
                    target_ESTest.toString(), target_ESTest_scaffolding.toString());
                final String classpath = getClassPath(targetDir, testDir);
                final Map<String, String> environmentVariables1 = new HashMap<>();
                environmentVariables1.put("CLASSPATH", classpath);
                final int exitValue = executeProcess(command, environmentVariables1);

                // コンパイルに失敗した場合には以降の処理をしない
                if (exitValue != 0) {
                  return;
                }

                // テストをデータベースに保存
                try {
                  final int methodID = Integer.valueOf(targetDirName);
                  final String text1 = Files.readString(target_ESTest, StandardCharsets.UTF_8);
                  final String text2 = Files.readString(target_ESTest_scaffolding,
                      StandardCharsets.UTF_8);
                  JavaMethodDAO.SINGLETON.setTests(methodID, text1, text2);
                } catch (final IOException e) {
                  e.printStackTrace();
                  System.exit(0);
                }
              }
            }
          });
        }
      }

      executorService.shutdown();
      executorService.awaitTermination(10, TimeUnit.MINUTES);

    } catch (final IOException | InterruptedException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }
}
