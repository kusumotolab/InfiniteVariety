package iv.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;
import org.mockito.Mockito;
import iv.IVConfig;
import iv.JavaVersion;
import iv.data.JavaMethod;

public class JavaFileVisitorTest {

  private static final String methodCode_CheckingTypeParameterHandling = //
      "public class Class1 {" + //
          "  public List<String> method1(Set<Integer> parameter1) {" + //
          "    System.out.println(\"hoge\");" + //
          "    return null;" + //
          "  }" + //
          "}";//

  private static final String methodCode_CheckingThrowStatement = //
      "public class Class1 {" + //
          "  public int method1(int a) {" + //
          "    throw new NullPointerException();" + //
          "    return 1;" + //
          "  }" + //
          "}";//

  // checking variable normalization
  private static final String methodCode_CheckingHashCalculation1 = //
      "public class Class1 {" + //
          "  public int method1(final Set<String> set1) {" + //
          "    int size1 = set1.size();" + //
          "    return size1;" + //
          "  }" +//
          "  public int method2(final Set<String> set2) {" + //
          "    int size2 = set2.size();" + //
          "    return size2;" + //
          "  }" +//
          "  public int method3(final Set<String> set3) {" + //
          "    int size3 = set3.size3();" + //
          "    return size3;" + //
          "  }" +//
          "}";

  // checking final removal
  private static final String methodCode_CheckingHashCalculation2 = //
      "public class Class1 {" + //
          "  public int method1(final int a) {" + //
          "    final int aa = a + 1;" + //
          "    return aa;" + //
          "  }" + //
          "  private int method2(int b) {" + //
          "    int bb = b + 1;" + //
          "    return bb;" + //
          " }" + //
          "}";

  // checking type argument removal
  private static final String methodCode_CheckingHashCalculation3 = //
      "public class Class1 {" + //
          "  public List<String> method1(String a) {" + //
          "    List<String> list = new ArrayList<>();" + //
          "    return list;" + //
          "  }" + //
          "  private List<String> method2(String b) {" + //
          "    List<String> list = new ArrayList<String>();" + //
          "    return list;" + //
          " }" + //
          "}";

  // checking annotation removal
  private static final String methodCode_CheckingHashCalculation4 = //
      "public class Class1 {" + //
          "  static public String method1(@NotNull String a) {" + //
          "    String aa = a + \"a\";" + //
          "    return aa;" + //
          "  }" + //
          "  public String method2(String b) {" + //
          "    String bb = b + \"b\";" + //
          "    return bb;" + //
          " }" + //
          "}";

  private static final String methodCode_CheckingHashCalculation5 = //
      "public class Class1 {" + //
          "  public String method1(String a) {" + //
          "    System.out.println(a);" + //
          "    return a;" + //
          "  }" + //
          "  public String method2(String b) throws IOException {" + //
          "    System.out.println(b);" + //
          "    return b;" + //
          "  }" + //
          "}";

  @Test
  public void test_checkingTypeParameterHandling() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingTypeParameterHandling);
    assertThat(methods).hasSize(1);
  }

  @Test
  public void test_checkingThrowStatement() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingThrowStatement);
    assertThat(methods).hasSize(0);
  }

  @Test
  public void test_checkingHashCalculation1() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingHashCalculation1);
    assertThat(methods).hasSize(3);

    final JavaMethod method1 = methods.get(0);
    final JavaMethod method2 = methods.get(1);
    final JavaMethod method3 = methods.get(2);
    assertThat(method1.getMD5()).isEqualTo(method2.getMD5());
    assertThat(method1.getMD5()).isNotEqualTo(method3.getMD5());
    assertThat(method2.getMD5()).isNotEqualTo(method3.getMD5());
  }

  @Test
  public void test_checkingHashCalculation2() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingHashCalculation2);
    assertThat(methods).hasSize(2);

    final JavaMethod method1 = methods.get(0);
    final JavaMethod method2 = methods.get(1);
    assertThat(method1.getMD5()).isEqualTo(method2.getMD5());
  }

  @Test
  public void test_checkingHashCalculation3() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingHashCalculation3);
    assertThat(methods).hasSize(2);

    final JavaMethod method1 = methods.get(0);
    final JavaMethod method2 = methods.get(1);
    assertThat(method1.getMD5()).isEqualTo(method2.getMD5());
  }


  @Test
  public void test_checkingHashCalculation4() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingHashCalculation4);
    assertThat(methods).hasSize(2);

    final JavaMethod method1 = methods.get(0);
    final JavaMethod method2 = methods.get(1);
    assertThat(method1.getMD5()).isEqualTo(method2.getMD5());
  }

  @Test
  public void test_checkingHashCalculation5() {
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingHashCalculation5);
    assertThat(methods).hasSize(1);
  }
}