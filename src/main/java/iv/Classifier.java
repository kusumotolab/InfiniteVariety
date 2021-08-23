package iv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iv.data.JavaMethod;
import iv.db.JavaMethodDAO;
import iv.util.Timer;

public class Classifier {

  private static final Logger logger = LoggerFactory.getLogger(Classifier.class);
  private final IVConfig config;

  public Classifier(final IVConfig config) {
    logger.trace("enter Classifier(JMCConfig)");
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

    final Classifier classifier = new Classifier(config);
    classifier.exec();

    timer.stop();
    logger.info("elapsed time: {}", timer);
  }

  public void exec() {
    logger.trace("enter exec()");

    // "-o"で指定されたディレクトリがすでにディレクトリとしてでは無く存在していた場合にはエラー
    final Path outputPath = config.getOutputPath();
    if (Files.exists(outputPath) && !Files.isDirectory(outputPath)) {
      System.err.println(
          "specified path already exists not as a directory: " + outputPath);
      System.exit(0);
    }

    // "-o"で指定されたディレクトリが存在しない場合には作成する
    if (!Files.exists(outputPath)) {
      try {
        Files.createDirectory(outputPath);
      } catch (final IOException e) {
        System.err.println("cannot make a directory: " + outputPath);
        System.exit(0);
      }
    }

    JavaMethodDAO.SINGLETON.initialize(config);
    final List<String> signatures = JavaMethodDAO.SINGLETON.getSignatures();

    final AtomicInteger dirIndex = new AtomicInteger(1);
    for (final String signature : signatures) {

      // 返値がないメソッド，引数がないメソッドは除外
      if (signature.startsWith("void(") || signature.endsWith("()")) {
        continue;
      }

      // 指定されたシグネチャを持つメソッドの数が2つ未満の場合は何もしない
      final List<JavaMethod> methods = JavaMethodDAO.SINGLETON.getMethods(signature);
      if (methods.size() < 2) {
        continue;
      }

      final Path dir = outputPath.resolve(dirIndex.getAndIncrement() + "." + signature);
      try {
        FileUtils.deleteDirectory(dir.toFile());
        Files.createDirectory(dir);

        final AtomicInteger fileIndex = new AtomicInteger(1);
        for (final JavaMethod method : methods) {
          final String fileName = "Class" + fileIndex.getAndIncrement();
          final Path file = dir.resolve(fileName + ".java");
          Files.writeString(file, method.getClassText(fileName), StandardCharsets.UTF_8);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
