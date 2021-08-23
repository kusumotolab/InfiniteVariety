package iv;

import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    final GitRepo gitRepo = new GitRepo(config);
    final RevCommit headCommit = gitRepo.getHeadCommit();
    final List<JavaMethod> javaMethods = gitRepo.getJavaMethods(headCommit);

    JavaMethodDAO.SINGLETON.initialize(config);
    JavaMethodDAO.SINGLETON.addMethods(javaMethods);
    JavaMethodDAO.SINGLETON.close();
  }
}
