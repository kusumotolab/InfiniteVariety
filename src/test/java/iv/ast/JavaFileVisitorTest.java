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
          "    return null;" + //
          "  }" + //
          "}";//

  private static final String methodCode_CheckingHashCalculation = //
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
  public void test_checkingHashCalculation(){
    final IVConfig config = Mockito.mock(IVConfig.class);
    when(config.getJavaVersion()).thenReturn(JavaVersion.V1_16);
    final RevCommit commit = Mockito.mock(RevCommit.class);
    when(commit.getName()).thenReturn("commit1");
    final JavaMethodExtractor extractor = new JavaMethodExtractor(config, null, commit);
    final List<JavaMethod> methods = extractor.getJavaMethods("",
        methodCode_CheckingHashCalculation);
    assertThat(methods).hasSize(3);

    final JavaMethod method1 = methods.get(0);
    final JavaMethod method2 = methods.get(1);
    final JavaMethod method3 = methods.get(2);
  }
}
