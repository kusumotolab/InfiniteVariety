package iv;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.kohsuke.args4j.Option;

public class IVConfig {

  private Path dbPath = null;
  private Path outputPath = null;
  private Path repoPath = null;
  private Path filePath = null;
  private JavaVersion javaVersion = JavaVersion.V1_8;
  private int lowerBound = 0;
  private int upperBound = Integer.MAX_VALUE;

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

  public Path getFilePath(){
    return this.filePath;
  }

  @Option(name = "-f", required = false, aliases = "--file", metaVar = "<file>",
      usage = "path to file or directory")
  public void setFilePath(final String path) {
    this.filePath = Paths.get(path)
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

  // TODO ループの上限および下限を指定するためのオプションを追加する必要あり
  // TODO とりあえず実装した．テストはまだ．
  public int getLowerBound() {
    return this.lowerBound;
  }

  @Option(name = "-l", required = false, aliases = "--lower-bound", metaVar = "<lowerBound>",
      usage = "lower bound of target method groups")
  public void setTargetLowerBound(final int lowerBound) {
    if(lowerBound < 0){
      System.err.println("an positive integer must be specified for target lower bound.");
      System.exit(0);
    }
    this.lowerBound = lowerBound;
  }

  public int getUpperBound() {
    return this.upperBound;
  }

  @Option(name = "-u", required = false, aliases = "--upper-bound", metaVar = "<upperBound>",
      usage = "upper bound of target method groups")
  public void setTargetUpperBound(final int upperBound) {
    if(upperBound < 0){
      System.err.println("an positive integer must be specified for target upper bound.");
      System.exit(0);
    }
    this.upperBound = upperBound;
  }
}
