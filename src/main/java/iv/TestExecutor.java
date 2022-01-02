package iv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
      // TODO このループを，指定された下限から上限の範囲のグループへのループに変更
      // IVConfigに下限と条件を指定するためのオプションを指定する必要あり
      final int lowerBound = config.getLowerBound();
      final int upperBound = config.getUpperBound();
      for (final Path groupDir : groupDirs) {

        // TODO 対象のメソッドグループ以外の処理をしないように変更．
        // TODO 実装はしたが，テストはまだ．
        final String groupDirName = groupDir.getFileName()
            .toString();
        if(!groupDirName.contains(".")){
          System.out.println(groupDirName + " is not a directory for method group");
          continue;
        }
        final int groupID = Integer.valueOf(groupDirName.substring(0, groupDirName.indexOf('.')));
        if(groupID < lowerBound || upperBound < groupID){
          continue;
        }

        // TODO メソッドのグループIDは，シグネチャが同じグループ内でリセットするように変更済み．
        // TODO 実装はしたが，テストはまだ．
        final AtomicInteger methodGroupID = new AtomicInteger(1);

        final List<Path> targetDirs = Files.list(groupDir)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());

        // テストが生成されているディレクトリ（メソッド）のみを取得
        final List<Path> compilableMethodDirs = targetDirs.stream()
            .filter(t -> JavaMethodDAO.SINGLETON.isCompilable(t.getFileName()
                .toString()))
            .collect(
                Collectors.toList());

        // 振る舞いが同じテスト群を格納するための入れ物を準備
        final Map<Integer, Set<Integer>> methodGroups = new HashMap<>();

        // マルチスレッド管理
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
            .availableProcessors());

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

            executorService.execute(new Runnable() {

              @Override
              public void run() {

                //System.out.println("checking compatibility between " + leftSourceDirName + " and " + rightSourceDirName);

                final List<String> command = Arrays.asList("java",
                    "org.junit.runner.JUnitCore",
                    "Target_ESTest");

                // leftのソースがrightのテストをパスするかを確認
                final String classpath1 = getClassPath(leftSourceDir, rightTestDir);
                final Map<String, String> environmentVariables1 = new HashMap<>();
                environmentVariables1.put("CLASSPATH", classpath1);
                final int junit1CommandExitValue = executeProcess(command, environmentVariables1);
                if (junit1CommandExitValue != 0) {
                  return;
                }

                // rightのソースがleftのテストをパスするかを確認
                final String classpath2 = getClassPath(rightSourceDir, leftTestDir);
                final Map<String, String> environmentVariables2 = new HashMap<>();
                environmentVariables2.put("CLASSPATH", classpath2);
                final int junit2CommandExitValue = executeProcess(command, environmentVariables2);
                if (junit2CommandExitValue != 0) {
                  return;
                }

                final int leftMethodID = Integer.valueOf(leftSourceDirName);
                final int rightMethodID = Integer.valueOf(rightSourceDirName);
                final Set<Integer> leftMethodGroup = methodGroups.get(leftMethodID);
                final Set<Integer> rightMethodGroup = methodGroups.get(rightMethodID);
                if (null == leftMethodGroup && null == rightMethodGroup) {
                  final Set<Integer> newGroup = new HashSet<>();
                  newGroup.add(leftMethodID);
                  newGroup.add(rightMethodID);
                  methodGroups.put(leftMethodID, newGroup);
                  methodGroups.put(rightMethodID, newGroup);
                } else if (null != leftMethodGroup && null == rightMethodGroup) {
                  leftMethodGroup.add(rightMethodID);
                  methodGroups.put(rightMethodID, leftMethodGroup);
                } else if (null == leftMethodGroup && null != rightMethodGroup) {
                  rightMethodGroup.add(leftMethodID);
                  methodGroups.put(leftMethodID, rightMethodGroup);
                } else if (null != leftMethodGroup && null != rightMethodGroup
                    && leftMethodGroup != rightMethodGroup) {
                  rightMethodGroup.forEach(rMethodID -> {
                    leftMethodGroup.add(rMethodID);
                    methodGroups.remove(rMethodID);
                    methodGroups.put(rMethodID, leftMethodGroup);
                  });
                }
              }
            });
          }
        }

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.MINUTES);

        final List<Set<Integer>> groups = methodGroups.values()
            .stream()
            .distinct()
            .filter(g -> JavaMethodDAO.SINGLETON.isDifferentSyntax(g))
            .collect(Collectors.toList());
        // TODO メソッドのグループIDは，シグネチャが同じメソッドグループでリセットするように変更する
        groups.forEach(g -> JavaMethodDAO.SINGLETON.setGroup(g, methodGroupID.getAndIncrement()));
      }

    } catch (final IOException | InterruptedException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }
}
