package iv.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.YieldStatement;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.eclipse.jgit.revwalk.RevCommit;
import iv.IVConfig;
import iv.data.JavaMethod;

public class JavaFileVisitor extends ASTVisitor {

  private final IVConfig config;
  private final String remoteUrl;
  private final RevCommit commit;
  private final String path;
  private final List<JavaMethod> javaMethods;
  private final AtomicInteger methodCount;
  private final Stack<List<SimpleName>> variableNodesStack;
  private final Stack<List<CharacterLiteral>> characterLiteralNodesStack;
  private final Stack<List<NumberLiteral>> numberLiteralNodesStack;
  private final Stack<List<StringLiteral>> stringLiteralNodesStack;
  private final Stack<List<Statement>> statementsStack;
  private final String[] javalangClasses = new String[] {
      "AbstractMethodError",//
      "Appendable",//
      "ArithmeticException",//
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
    this.methodCount = new AtomicInteger(0);
    this.characterLiteralNodesStack = new Stack<>();
    this.numberLiteralNodesStack = new Stack<>();
    this.stringLiteralNodesStack = new Stack<>();
    this.variableNodesStack = new Stack<>();
    this.statementsStack = new Stack<>();
  }

  public List<JavaMethod> getJavaMethods() {
    return Collections.unmodifiableList(javaMethods);
  }

  public int getAllMethodCount() {
    return methodCount.get();
  }

  @Override
  public boolean visit(final MethodDeclaration node) {

    // ?????????????????????????????????
    if (node.isConstructor()) {
      return false;
    }

    final List<?> thrownExceptionTypes = node.thrownExceptionTypes();
    if (null != thrownExceptionTypes && !thrownExceptionTypes.isEmpty()) {
      return false;
    }

    // ???????????????????????????????????????
    methodCount.getAndIncrement();

    // ??????????????????????????????????????????????????????
    characterLiteralNodesStack.push(new ArrayList<CharacterLiteral>());
    numberLiteralNodesStack.push(new ArrayList<NumberLiteral>());
    stringLiteralNodesStack.push(new ArrayList<StringLiteral>());
    variableNodesStack.push(new ArrayList<SimpleName>());

    // ????????????????????????????????????????????????
    final Optional<Type> returnTypeOptional = Optional.ofNullable(node.getReturnType2());
    final List<SingleVariableDeclaration> parameters = (List<SingleVariableDeclaration>) node.parameters();
    final Optional<Block> bodyOptional = Optional.ofNullable(node.getBody());

    // ??????????????????????????????????????????
    node.modifiers()
        .removeIf(m -> m instanceof MarkerAnnotation || m instanceof NormalAnnotation);

    // ??????????????????????????????
    node.modifiers()
        .removeIf(m -> m instanceof Modifier);
    parameters.stream()
        .map(p -> p.modifiers())
        .forEach(m -> m.clear());

    // Javadoc?????????
    Optional.ofNullable(node.getJavadoc())
        .ifPresent(j -> j.delete());

    //?????????????????????????????????????????????????????????????????????
    // ???????????????node.getBody()?????????????????????????????????????????????????????????????????????
    final String methodName = node.getName()
        .getIdentifier();
    final String rawText = node.toString();

    // ?????????????????????????????????????????????
    if (bodyOptional.isEmpty()) {
      characterLiteralNodesStack.pop();
      numberLiteralNodesStack.pop();
      stringLiteralNodesStack.pop();
      variableNodesStack.pop();
      return false;
    }
    //?????????????????????????????????????????????????????????????????????????????????
    isTarget = true;
    returnTypeOptional.ifPresent(r -> r.accept(this));
    parameters.forEach(p -> p.accept(this));
    if (!isTarget) {
      characterLiteralNodesStack.pop();
      numberLiteralNodesStack.pop();
      stringLiteralNodesStack.pop();
      variableNodesStack.pop();
      return false;
    }

    //???????????????????????????????????????????????????????????????????????????????????????
    //??????????????????????????????????????????if????????????????????????????????????
    statementsStack.push(new ArrayList<Statement>());
    bodyOptional.ifPresent(b -> b.accept(this));
    final List<Statement> statements = statementsStack.pop();
    if (!isTarget) {
      characterLiteralNodesStack.pop();
      numberLiteralNodesStack.pop();
      stringLiteralNodesStack.pop();
      variableNodesStack.pop();
      return false;
    }

    // ????????????????????????????????????????????????
    final List<CharacterLiteral> characterLiteralNodes = characterLiteralNodesStack.pop();
    characterLiteralNodes.forEach(n -> n.setCharValue('$'));
    ;
    final List<NumberLiteral> numberLiteralNodes = numberLiteralNodesStack.pop();
    numberLiteralNodes.forEach(n -> n.setToken("0"));
    final List<StringLiteral> stringLiteralNodes = stringLiteralNodesStack.pop();
    stringLiteralNodes.forEach(n -> n.setLiteralValue("$string"));
    final List<SimpleName> variableNodes = variableNodesStack.pop();
    variableNodes.forEach(n -> n.setIdentifier("$variable"));
    node.getName()
        .setIdentifier("$method");
    final String normalizedText = node.toString();

    // ?????????????????????????????????????????????????????????, ???????????????????????????????????????????????????????????????????????????????????????
    final String returnType = returnTypeOptional.map(ASTNode::toString)
        .orElse("void");
    final int branches = getBranchNumber(statements);
    final CompilationUnit rootNode = (CompilationUnit) node.getRoot();
    final int startLine = rootNode.getLineNumber(node.getStartPosition());
    final int endLine = rootNode.getLineNumber(node.getStartPosition() + node.getLength());
    final JavaMethod method = new JavaMethod(returnType, methodName, rawText, normalizedText,
        statements.size(), branches, path, startLine, endLine, remoteUrl, commit);

    // ???????????????????????????
    for (final SingleVariableDeclaration parameter : parameters) {
      final Type type = parameter.getType();
      method.addParameter(type.toString());
    }

    javaMethods.add(method);
    return false;
  }

  private int getBranchNumber(final List<Statement> statements) {
    return (int) statements.stream()
        .filter(s -> s.getClass() == DoStatement.class || s.getClass() == ForStatement.class
            || s.getClass() == EnhancedForStatement.class
            || s.getClass() == IfStatement.class || s.getClass() == SwitchStatement.class
            || s.getClass() == WhileStatement.class)
        .count();
  }

  @Override
  public boolean visit(final ArrayType node) {
    // ???????????????????????????????????????????????????????????????????????????????????????
    // ????????????????????????????????????????????????????????????????????????
    return super.visit(node);
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
    // ??????????????????????????????????????????
    return super.visit(node);
  }

  @Override
  public boolean visit(final PrimitiveType node) {
    // ???????????????????????????????????????????????????????????????
    // isTarget???true???????????????????????????????????????false??????????????????????????????????????????????????????????????????
    return super.visit(node);
  }

  @Override
  public boolean visit(final QualifiedType node) {
    isTarget = false;
    return false;
  }

  @Override
  public boolean visit(final SimpleType node) {
    final String typeName = node.toString();
    // java.lang????????????java.util??????????????????????????????????????????????????????????????????????????????????????????
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
    if (!variableNodesStack.isEmpty()) {
      final List<SimpleName> normalizationTargetNodes = variableNodesStack.peek();
      normalizationTargetNodes.add(node);
    }
    return false;
  }

  @Override
  public boolean visit(final CharacterLiteral node) {
    if (!characterLiteralNodesStack.isEmpty()) {
      final List<CharacterLiteral> normalizationTargetNodes = characterLiteralNodesStack.peek();
      normalizationTargetNodes.add(node);
    }
    return false;
  }

  @Override
  public boolean visit(final NumberLiteral node) {
    if (!numberLiteralNodesStack.isEmpty()) {
      final List<NumberLiteral> normalizationTargetNodes = numberLiteralNodesStack.peek();
      normalizationTargetNodes.add(node);
    }
    return false;
  }

  @Override
  public boolean visit(final StringLiteral node) {
    if (!stringLiteralNodesStack.isEmpty()) {
      final List<StringLiteral> normalizationTargetNodes = stringLiteralNodesStack.peek();
      normalizationTargetNodes.add(node);
    }
    return false;
  }

  @Override
  public boolean visit(final MethodInvocation node) {
    final Optional<Expression> expression = Optional.ofNullable(node.getExpression());
    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    if (expression.isEmpty()) {
      isTarget = false;
      return false;
    }
    expression.ifPresent(e -> e.accept(this));
    node.arguments()
        .stream()
        .forEach(n -> ((ASTNode) n).accept(this));
    return false;
  }

  @Override
  public boolean visit(final ReturnStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    // return???????????????????????????????????????void????????????????????????
    final Expression operand = node.getExpression();
    if (null == operand) {
      isTarget = false;
      return false;
    }

    // ???????????????????????????????????????????????????
    // return?????????????????????????????????
    // return???????????????????????????????????????????????????return????????????????????????null
    final ASTNode parent = node.getParent();
    final ASTNode grandParent = parent.getParent();
    if (parent instanceof Block && 1 == ((Block) parent).statements()
        .size() && MethodDeclaration.class == grandParent.getClass()
        && (SimpleName.class == operand.getClass() || NullLiteral.class == operand.getClass()
        || CharacterLiteral.class == operand.getClass() || StringLiteral.class == operand.getClass()
        || NumberLiteral.class == operand.getClass())) {
      isTarget = false;
      return false;
    }

    operand.accept(this);
    return false;
  }

  @Override
  public boolean visit(final AssertStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final BreakStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final ContinueStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final DoStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final EmptyStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final ExpressionStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final ForStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final IfStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final SwitchStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final SynchronizedStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final ThrowStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    // ??????????????????????????????Throw???????????????????????????????????????
    final ASTNode parent = node.getParent();
    final ASTNode grandParent = parent.getParent();
    if (parent instanceof Block && 1 == ((Block) parent).statements()
        .size() && MethodDeclaration.class == grandParent.getClass()) {
      isTarget = false;
      return false;
    }

    // ????????????throw???????????????????????????????????????
    isTarget = false;

    return super.visit(node);
  }

  @Override
  public boolean visit(final TypeDeclarationStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final TryStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final VariableDeclarationStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    // ??????????????????
    node.modifiers()
        .removeIf(m -> m instanceof Modifier);

    return super.visit(node);
  }

  @Override
  public boolean visit(final WhileStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public boolean visit(final YieldStatement node) {

    if (!statementsStack.isEmpty()) {
      statementsStack.peek()
          .add(node);
    }

    return super.visit(node);
  }

  @Override
  public void endVisit(final ClassInstanceCreation node) {
    // ??????????????????????????????????????????????????????
    final Type type = node.getType();
    if (type.isParameterizedType()) {
      ((ParameterizedType) type).typeArguments()
          .clear();
    }
  }
}
