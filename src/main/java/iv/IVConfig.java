package iv;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.kohsuke.args4j.Option;

public class IVConfig {

  private Path dbPath = null;
  private Path outputPath = null;
  private Path repoPath = null;
  private JavaVersion javaVersion = JavaVersion.V1_8;

  public Path getDatabase() {
    return this.dbPath;
  }

  @Option(name = "-d", required = false, aliases = "--database", metaVar = "<database>",
      usage = "path to database")
  public void setDesPath(final String dbPath) {
    this.dbPath = Paths.get(dbPath)
        .toAbsolutePath();
  }

  public Path getOutputPath() {
    return this.outputPath;
  }

  @Option(name = "-o", required = false, aliases = "--output", metaVar = "<output>",
      usage = "path to output")
  public void setOutputPath(final String path) {
    this.outputPath = Paths.get(path)
        .toAbsolutePath();
  }

  public Path getRepoPath() {
    return this.repoPath;
  }

  @Option(name = "-r", required = false, aliases = "--repository", metaVar = "<repository>",
      usage = "path to Git repository")
  public void setRepoPath(final String path) {
    this.repoPath = Paths.get(path)
        .toAbsolutePath();
  }

  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  @Option(name = "-j", required = false, aliases = "--java-version", metaVar = "<version>",
      usage = "java version of target source files")
  public void setJavaVersion(final String versionText) {
    this.javaVersion = JavaVersion.get(versionText);
    if (null == this.javaVersion) {
      System.err.println("an invalid value is specified for option \"-j\".");
      System.err.println("specify your Java version in \"1.4\" ~ \"1.13\".");
      System.exit(1);
    }
  }
}
