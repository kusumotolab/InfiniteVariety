package iv.ast;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.revwalk.RevCommit;
import iv.IVConfig;
import iv.JavaVersion;
import iv.data.JavaMethod;

public class JavaMethodExtractor {

  private final IVConfig config;
  private final String remoteUrl;
  private final RevCommit commit;
  private final AtomicLong totalMethodCount;

  public JavaMethodExtractor(final IVConfig config, final String remoteUrl,
      final RevCommit commit) {
    this.config = config;
    this.remoteUrl = remoteUrl;
    this.commit = commit;
    this.totalMethodCount = new AtomicLong(0);
  }

  public List<JavaMethod> getJavaMethods(final String path, final String text) {
    final ASTParser parser = createNewParser();
    parser.setSource(text.toCharArray());
    final CompilationUnit ast = (CompilationUnit) parser.createAST(null);

    // 与えられたASTに問題があるときは何もしない
    final IProblem[] problems = ast.getProblems();
    if (null == problems || 0 < problems.length) {
      return Collections.emptyList();
    }

    final JavaFileVisitor visitor = new JavaFileVisitor(config, remoteUrl, commit, path);
    ast.accept(visitor);
    totalMethodCount.addAndGet(visitor.getAllMethodCount());
    return visitor.getJavaMethods();
  }

  public long getTotalMethodCount(){
    return totalMethodCount.get();
  }

  private ASTParser createNewParser() {
    ASTParser parser = ASTParser.newParser(AST.JLS16);
    final JavaVersion javaVersion = this.config.getJavaVersion();
    final Map<String, String> options = javaVersion.getOptions();
    parser.setCompilerOptions(options);

    // TODO: Bindingが必要か検討
    parser.setResolveBindings(false);
    parser.setBindingsRecovery(false);
    parser.setEnvironment(null, null, null, true);

    return parser;
  }
}
