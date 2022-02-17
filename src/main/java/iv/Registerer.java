package iv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iv.ast.JavaMethodExtractor;
import iv.data.JavaMethod;
import iv.db.JavaMethodDAO;
import iv.git.GitRepo;
import iv.util.Timer;

public class Registerer {

  private static final Logger logger = LoggerFactory.getLogger(Registerer.class);
  private final IVConfig config;

  public Registerer(final IVConfig config) {
    logger.trace("enter Registerer(JMCConfig)");
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

    final Registerer registerer = new Registerer(config);
    registerer.exec();

    timer.stop();
    logger.info("elapsed time: {}", timer);
  }

  public void exec() {
    logger.trace("enter exec()");

    final List<JavaMethod> javaMethods = new ArrayList<>();
    final Path filePath = config.getFilePath();
    final Path repoPath = config.getRepoPath();
    if (null == filePath && null == repoPath) {
      System.err.println("either of \"-f\" or \"-r\" must be specified.");
      System.exit(0);
    } else if (null != filePath && null != repoPath) {
      System.err.println("both of \"-f\" and \"-r\" must not be specified.");
      System.err.println("please specify either of them.");
      System.exit(0);
    } else if (null != filePath) {
      javaMethods.addAll(getJavaMethods(filePath));
    } else if (null != repoPath) {
      final GitRepo gitRepo = new GitRepo(config);
      final RevCommit headCommit = gitRepo.getHeadCommit();
      javaMethods.addAll(gitRepo.getJavaMethods(headCommit));
    }

    JavaMethodDAO.SINGLETON.initialize(config);
    JavaMethodDAO.SINGLETON.addMethods(javaMethods.stream()
        .filter(m -> !m.isTest())
        .filter(m -> !m.isToy())
        .collect(
            Collectors.toList()));
    final long tests = javaMethods.stream()
        .filter(m -> m.isTest())
        .count();
    final long toys = javaMethods.stream()
        .filter(m -> m.isToy())
        .count();
    System.out.println("tests: " + tests);
    System.out.println("toys: " + toys);
    JavaMethodDAO.SINGLETON.close();
  }

  private List<JavaMethod> getJavaMethods(final Path dir) {
    final List<JavaMethod> javaMethods = new ArrayList<>();

    // dirがファイルのとき
    if (Files.isRegularFile(dir)) {
      final String fileName = dir.toString();
      if (fileName.endsWith(".java")) {
        final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, null);
        try {
          final String text = Files.readString(dir);
          final List<JavaMethod> methods = extractor.getJavaMethods(dir.toString(), text);
          javaMethods.addAll(methods);
        } catch (final IOException e) {
          System.err.println(e.getMessage());
        }
      }
    }

    // dirがディレクトリのとき
    else if (Files.isDirectory(dir)) {

      try {
        Files.list(dir)
            .forEach(c -> javaMethods.addAll(getJavaMethods(c)));
      } catch (final IOException e) {
        System.err.println(e.getMessage());
      }
    }

    return javaMethods;
  }


}
