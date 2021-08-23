package iv.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iv.IVConfig;
import iv.ast.JavaMethodExtractor;
import iv.data.JavaMethod;

public class GitRepo {

  private static final Logger log = LoggerFactory.getLogger(GitRepo.class);
  public final IVConfig config;
  private FileRepository repository;
  public GitRepo(final IVConfig config) {
    log.trace("enter GitRepo(JMCConfig)");

    this.config = config;
    final Path repoPath = config.getRepoPath();
    try {
      this.repository = new FileRepository(repoPath.resolve(".git")
          .toString());
    } catch (final IOException e) {
      e.printStackTrace();
      System.err.println("cannot access to " + repoPath);
      System.exit(0);
    }
  }

  /**
   * 引数で与えられたコミットハッシュの最初の7文字を返す
   *
   * @param anyObjectId
   * @return
   */
  public static String getAbbreviatedID(final AnyObjectId anyObjectId) {
    return null == anyObjectId ? null : anyObjectId.abbreviate(7)
        .name();
  }

  public RevCommit getHeadCommit() {
    log.trace("enter getHeadCommit()");
    return this.getCommit(Constants.HEAD);
  }

  /**
   * 引数で与えられたコミットIDを持つRevCommitを返す．引数で与えられたコミットがない場合にはnullを返す．
   *
   * @param commit
   * @return
   */
  public RevCommit getCommit(final String commit) {
    log.trace("enter getCommit(String)");
    final ObjectId commitId = this.getObjectId(commit);
    final RevCommit revCommit = this.getRevCommit(commitId);
    return revCommit;
  }

  private ObjectId getObjectId(final String name) {
    log.trace("enter getObjectId(String=\"{}\")", name);

    if (null == name) {
      return null;
    }

    try {
      final ObjectId objectId = this.repository.resolve(name);
      return objectId;
    } catch (final RevisionSyntaxException e) {
      log.error("FileRepository#resolve is invoked with an incorrect formatted argument");
      log.error(e.getMessage());
    } catch (final AmbiguousObjectException e) {
      log.error("FileRepository#resolve is invoked with an an ambiguous object ID");
      log.error(e.getMessage());
    } catch (final IncorrectObjectTypeException e) {
      log.error("FileRepository#resolve is invoked with an ID of inappropriate object type");
      log.error(e.getMessage());
    } catch (final IOException e) {
      log.error("cannot access to repository \"" + this.repository.getWorkTree()
          .toString());
    }
    return null;
  }

  public RevCommit getRevCommit(final AnyObjectId commitId) {
    log.trace("enter getRevCommit(AnyObjectId=\"{}\")", getAbbreviatedID(commitId));

    if (null == commitId) {
      return null;
    }

    try (final RevWalk revWalk = new RevWalk(this.repository)) {
      final RevCommit commit = revWalk.parseCommit(commitId);
      return commit;
    } catch (final IOException e) {
      log.error("cannot parse commit \"{}\"", getAbbreviatedID(commitId));
      log.error(e.getMessage());
      return null;
    }
  }

  public List<JavaMethod> getJavaMethods(final RevCommit commit) {
    final List<JavaMethod> javaMethods = new ArrayList<>();
    final String remoteUrl = repository.getConfig()
        .getString("remote", "origin", "url");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(this.config, remoteUrl, commit);
    final RevTree tree = commit.getTree();
    final TreeWalk walker = new TreeWalk(repository);
    try {
      walker.addTree(tree);
      walker.setRecursive(true);
      walker.setFilter(PathSuffixFilter.create(".java"));
      while (walker.next()) {
        final String path = walker.getPathString();
        final ObjectId blobId = walker.getObjectId(0);
        try (final ObjectReader objectReader = repository.newObjectReader()) {
          final ObjectLoader objectLoader = objectReader.open(blobId);
          final byte[] bytes = objectLoader.getBytes();
          final String text = new String(bytes, StandardCharsets.UTF_8);
          final List<JavaMethod> methods = extractor.getJavaMethods(path, text);
          javaMethods.addAll(methods);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return javaMethods;
  }
}
