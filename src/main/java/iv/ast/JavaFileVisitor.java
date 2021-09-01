package iv.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jgit.revwalk.RevCommit;
import iv.IVConfig;
import iv.data.JavaMethod;

public class JavaFileVisitor extends ASTVisitor {

  private final IVConfig config;
  private final String remoteUrl;
  private final RevCommit commit;
  private final String path;
  private final List<JavaMethod> javaMethods;
  private boolean isTarget;

  public JavaFileVisitor(final IVConfig config, final String remoteUrl, final RevCommit commit,
      final String path) {
    this.config = config;
    this.remoteUrl = remoteUrl;
    this.commit = commit;
    this.path = path;
    this.isTarget = true;
    this.javaMethods = new ArrayList<>();
  }

  public List<JavaMethod> getJavaMethods() {
    return Collections.unmodifiableList(javaMethods);
  }

  @Override
  public boolean visit(final MethodDeclaration node) {

    // 返値，引数，ボディのノードを取得
    final Optional<Type> returnTypeOptional = Optional.ofNullable(node.getReturnType2());
    final List<SingleVariableDeclaration> parameters = (List<SingleVariableDeclaration>) node.parameters();
    final Optional<Block> bodyOptional = Optional.ofNullable(node.getBody());

    //ビジターを利用して条件を満たすかチェック
    isTarget = true;
    returnTypeOptional.ifPresent(r -> r.accept(this));
    parameters.forEach(p -> p.accept(this));
    bodyOptional.ifPresent(b -> b.accept(this));
    if (!isTarget) {
      return false;
    }

    // アノテーションを削除
    node.modifiers()
        .stream()
        .filter(m -> m instanceof MarkerAnnotation)
        .forEach(m -> ((MarkerAnnotation) m).delete());

    // 返値，メソッド名，メソッド全体の文字列, パスを利用してメソッドオブジェクトを生成
    final String returnType = returnTypeOptional.map(ASTNode::toString)
        .orElse("void");
    final String methodName = node.getName()
        .getIdentifier();
    final String methodText = node.toString();

    final CompilationUnit rootNode = (CompilationUnit) node.getRoot();
    final int startLine = rootNode.getLineNumber(node.getStartPosition());
    final int endLine = rootNode.getLineNumber(node.getStartPosition() + node.getLength());
    final JavaMethod method = new JavaMethod(returnType, methodName, methodText, path, startLine,
        endLine, remoteUrl, commit.getName());

    // 引数の型を追加する
    for (final SingleVariableDeclaration parameter : parameters) {
      final Type type = parameter.getType();
      method.addParameter(type.toString());
    }

    javaMethods.add(method);
    return false;
  }

  @Override
  public boolean visit(final ArrayType node) {
    // 配列型のときは，ここでは対象の型かどうかの判定は行わない．
    // 要素である型にビジターを訪れたときに判定される．
    return super.visit(node);
  }

  @Override
  public boolean visit(final IntersectionType node) {
    isTarget = false;
    return super.visit(node);
  }

  @Override
  public boolean visit(final NameQualifiedType node) {
    isTarget = false;
    return super.visit(node);
  }

  @Override
  public boolean visit(final ParameterizedType node) {
    isTarget = false;
    return super.visit(node);
  }

  @Override
  public boolean visit(final PrimitiveType node) {
    // プリミティブ型は対象の型なので何もしない．
    // isTargetにtrueをいれると，もしそれまでにfalseになっている場合にその情報が失われてしまう．
    return super.visit(node);
  }

  @Override
  public boolean visit(final QualifiedType node) {
    isTarget = false;
    return super.visit(node);
  }

  @Override
  public boolean visit(final SimpleType node) {
    final String typeName = node.toString();
    if (!typeName.equals("String")) {
      isTarget = false;
    }
    return super.visit(node);
  }

  @Override
  public boolean visit(final UnionType node) {
    isTarget = false;
    return super.visit(node);
  }

  @Override
  public boolean visit(final WildcardType node) {
    isTarget = false;
    return super.visit(node);
  }
}
