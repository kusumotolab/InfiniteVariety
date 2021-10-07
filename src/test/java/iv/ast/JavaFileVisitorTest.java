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
}
