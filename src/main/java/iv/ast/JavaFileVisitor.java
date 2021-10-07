package iv.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
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
  private final Stack<List<SimpleName>> normalizationTargetNodesStack;
  private final String[] javalangClasses = new String[] {
      "AbstractMethodError",//
      "Appendable",//
      " ArithmeticException",//
      "ArrayIndexOutOfBoundsException",//
      "ArrayStoreException",//
      "AssertionError",//
      "AutoCloseable",//
      "Boolean",//
      "BootstrapMethodError",//
      "Byte",//
      "Character",//
      "CharSequence",//
      "Class",//
      "ClassCastException",//
      "ClassCircularityError",//
      "ClassFormatError",//
      "ClassLoader",//
      "ClassNotFoundException",//
      "ClassValue",//
      "Cloneable",//
      "CloneNotSupportedException",//
      "Comparable",//
      "Compiler",//
      "Deprecated",//
      "Double",//
      "Enum",//
      "EnumConstantNotPresentException",//
      "Error",//
      "Exception",//
      "ExceptionInInitializerError",//
      "Float",//
      "FunctionalInterface",//
      "IllegalAccessError",//
      "IllegalAccessException",//
      "IllegalArgumentException",//
      "IllegalCallerException",//
      "IllegalMonitorStateException",//
      "IllegalStateException",//
      "IllegalThreadStateException",//
      "IncompatibleClassChangeError",//
      "IndexOutOfBoundsException",//
      "InheritableThreadLocal",//
      "InstantiationError",//
      "InstantiationException",//
      "Integer",//
      "InternalError",//
      "InterruptedException",//
      "Iterable",//
      "LayerInstantiationException",//
      "LinkageError",//
      "Long",//
      "Math",//
      "Module",//
      "ModuleLayer",//
      "NegativeArraySizeException",//
      "NoClassDefFoundError",//
      "NoSuchFieldError",//
      "NoSuchFieldException",//
      "NoSuchMethodError",//
      "NoSuchMethodException",//
      "NullPointerException",//
      "Number",//
      "NumberFormatException",//
      "Object",//
      "OutOfMemoryError",//
      "Override",//
      "Package",//
      "Process",//
      "ProcessBuilder",//
      "ProcessHandle",//
      "Readable",//
      "Record",//
      "ReflectiveOperationException",//
      "Runnable",//
      "Runtime",//
      "RuntimeException",//
      "RuntimePermission",//
      "SafeVarargs",//
      "SecurityException",//
      "SecurityManager",//
      "Short",//
      "StackOverflowError",//
      "StackTraceElement",//
      "StackWalker",//
      "StrictMath",//
      "String",//
      "StringBuffer",//
      "StringBuilder",//
      "SuppressWarnings",//
      "System",//
      "Thread",//
      "ThreadDeath",//
      "ThreadGroup",//
      "ThreadLocal",//
      "Throwable",//
      "TypeNotPresentException",//
      "UnknownError",//
      "UnsatisfiedLinkError",//
      "UnsupportedClassVersionError",//
      "UnsupportedOperationException",//
      "VerifyError",//
      "VirtualMachineError",//
      "Void"//
  };

  private final String[] javautilClasses = new String[] {
      "AbstractCollection",//
      "AbstractList",//
      "AbstractMap",//
      "AbstractQueue",//
      "AbstractSequentialList",//
      "AbstractSet",//
      "ArrayDeque",//
      "ArrayList",//
      "Arrays",//
      "Base64",//
      "BitSet",//
      "Calendar",//
      "Collection",//
      "Collections",//
      "Comparator",//
      "ConcurrentModificationException",//
      "Currency",//
      "Date",//
      "Deque",//
      "Dictionary",//
      "DoubleSummaryStatistics",//
      "DuplicateFormatFlagsException",//
      "EmptyStackException",//
      "Enumeration",//
      "EnumMap",//
      "EnumSet",//
      "EventListener",//
      "EventListenerProxy",//
      "EventObject",//
      "FormatFlagsConversionMismatchException",//
      "Formattable",//
      "Formatter",//
      "FormatterClosedException",//
      "GregorianCalendar",//
      "HashMap",//
      "HashSet",//
      "Hashtable",//
      "HexFormat",//
      "IdentityHashMap",//
      "IllegalFormatCodePointException",//
      "IllegalFormatConversionException",//
      "IllegalFormatException",//
      "IllegalFormatFlagsException",//
      "IllegalFormatPrecisionException",//
      "IllegalFormatWidthException",//
      "IllformedLocaleException",//
      "InputMismatchException",//
      "IntSummaryStatistics",//
      "InvalidPropertiesFormatException",//
      "Iterator",//
      "LinkedHashMap",//
      "LinkedHashSet",//
      "LinkedList",//
      "List",//
      "ListIterator",//
      "ListResourceBundle",//
      "Locale",//
      "LongSummaryStatistics",//
      "Map",//
      "MissingFormatArgumentException",//
      "MissingFormatWidthException",//
      "MissingResourceException",//
      "NavigableMap",//
      "NavigableSet",//
      "NoSuchElementException",//
      "Objects",//
      "Observable",//
      "Observer",//
      "Optional",//
      "OptionalDouble",//
      "OptionalInt",//
      "OptionalLong",//
      "PrimitiveIterator",//
      "PriorityQueue",//
      "Properties",//
      "PropertyPermission",//
      "PropertyResourceBundle",//
      "Queue",//
      "Random",//
      "RandomAccess",//
      "ResourceBundle",//
      "Scanner",//
      "ServiceConfigurationError",//
      "ServiceLoader",//
      "Set",//
      "SimpleTimeZone",//
      "SortedMap",//
      "SortedSet",//
      "Spliterator",//
      "Spliterators",//
      "SplittableRandom",//
      "Stack",//
      "StringJoiner",//
      "StringTokenizer",//
      "Timer",//
      "TimerTask",//
      "TimeZone",//
      "TooManyListenersException",//
      "TreeMap",//
      "TreeSet",//
      "UnknownFormatConversionException",//
      "UnknownFormatFlagsException",//
      "UUID",//
      "Vector",//
      "WeakHashMap",//
  };

  private boolean isTarget;

  public JavaFileVisitor(final IVConfig config, final String remoteUrl, final RevCommit commit,
      final String path) {
    this.config = config;
    this.remoteUrl = remoteUrl;
    this.commit = commit;
    this.path = path;
    this.isTarget = true;
    this.javaMethods = new ArrayList<>();
    this.normalizationTargetNodesStack = new Stack<>();
  }

  public List<JavaMethod> getJavaMethods() {
    return Collections.unmodifiableList(javaMethods);
  }

  @Override
  public boolean visit(final MethodDeclaration node) {

    // 正規化する変数名を表すノードを記録するための処理
    normalizationTargetNodesStack.push(new ArrayList<SimpleName>());

    // アノテーションを削除
    node.modifiers()
        .removeIf(m -> m instanceof MarkerAnnotation || m instanceof NormalAnnotation);

    // staticを削除
    node.modifiers()
        .removeIf(m -> m instanceof Modifier && ((Modifier) m).isStatic());

    // privateを削除
    node.modifiers()
        .removeIf(m -> m instanceof Modifier && ((Modifier) m).isPrivate());

    // Javadocを削除
    Optional.ofNullable(node.getJavadoc())
        .ifPresent(j -> j.delete());

    //　このメソッドの生文字列を取り出しておく
    // この処理はnode.getBody()に対するビジター処理よりも先になければいけない
    final String rawText = node.toString();

    // 返値，引数，ボディのノードを取得
    final Optional<Type> returnTypeOptional = Optional.ofNullable(node.getReturnType2());
    final List<SingleVariableDeclaration> parameters = (List<SingleVariableDeclaration>) node.parameters();
    final Optional<Block> bodyOptional = Optional.ofNullable(node.getBody());

    //ビジターを利用して，返値と引数が条件を満たすかチェック
    isTarget = true;
    returnTypeOptional.ifPresent(r -> r.accept(this));
    parameters.forEach(p -> p.accept(this));
    if (!isTarget) {
      normalizationTargetNodesStack.pop();
      return false;
    }

    //ビジターを利用して，メソッドボディが条件を満たすかチェック
    //返値と引数のチェックのあとのif文を取り除いてはいけない
    bodyOptional.ifPresent(b -> b.accept(this));
    if (!isTarget) {
      normalizationTargetNodesStack.pop();
      return false;
    }

    // このメソッドの正規化文字列を取得
    final List<SimpleName> normalizationTargetNodes = normalizationTargetNodesStack.pop();
    normalizationTargetNodes.forEach(n -> n.setIdentifier("$variable"));
    node.getName().setIdentifier("$method");
    final String normalizedText = node.toString();

    // 返値，メソッド名，メソッド全体の文字列, 正規化後の文字列，パスを利用してメソッドオブジェクトを生成
    final String returnType = returnTypeOptional.map(ASTNode::toString)
        .orElse("void");
    final String methodName = node.getName()
        .getIdentifier();
    final CompilationUnit rootNode = (CompilationUnit) node.getRoot();
    final int startLine = rootNode.getLineNumber(node.getStartPosition());
    final int endLine = rootNode.getLineNumber(node.getStartPosition() + node.getLength());
    final JavaMethod method = new JavaMethod(returnType, methodName, rawText, normalizedText, path,
        startLine, endLine, remoteUrl, commit.getName());

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
    return false;
  }

  @Override
  public boolean visit(final IntersectionType node) {
    isTarget = false;
    return false;
  }

  @Override
  public boolean visit(final NameQualifiedType node) {
    isTarget = false;
    return false;
  }

  @Override
  public boolean visit(final ParameterizedType node) {
    // 型引数についてはなにもしない
    return false;
  }

  @Override
  public boolean visit(final PrimitiveType node) {
    // プリミティブ型は対象の型なので何もしない．
    // isTargetにtrueをいれると，もしそれまでにfalseになっている場合にその情報が失われてしまう．
    return false;
  }

  @Override
  public boolean visit(final QualifiedType node) {
    isTarget = false;
    return false;
  }

  @Override
  public boolean visit(final SimpleType node) {
    final String typeName = node.toString();
    // java.langもしくはjava.utilのいずれのクラス名にも一致しない場合は対象外のクラスと見なす
    if (!Stream.concat(Arrays.stream(javalangClasses), Arrays.stream(javautilClasses))
        .anyMatch(c -> c.equals(typeName))) {
      isTarget = false;
    }
    return false;
  }

  @Override
  public boolean visit(final UnionType node) {
    isTarget = false;
    return false;
  }

  @Override
  public boolean visit(final WildcardType node) {
    isTarget = false;
    return false;
  }

  @Override
  public boolean visit(final SimpleName node) {
    System.out.println(node.getIdentifier());
    if (!normalizationTargetNodesStack.isEmpty()) {
      final List<SimpleName> normalizationTargetNodes = normalizationTargetNodesStack.peek();
      normalizationTargetNodes.add(node);
    }
    return false;
  }

  @Override
  public boolean visit(final MethodInvocation node) {
    final Optional<Expression> expression = Optional.ofNullable(node.getExpression());
    expression.ifPresent(e -> e.accept(this));
    node.arguments()
        .stream()
        .forEach(n -> ((ASTNode) n).accept(this));
    return false;
  }
}
