package iv.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class JavaMethodTest {

  @Test
  public void test_isTest1() {
    final JavaMethod method = new JavaMethod("return", "name", "rawText", "normalizedText", 1,
        0, "aaa/test/bbb/C.java", 1, 10, "repository", null);
    assertThat(method.isTest()).isTrue();
  }

  @Test
  public void test_isTest2() {
    final JavaMethod method = new JavaMethod("return", "name", "rawText", "normalizedText", 1,
        0, "aaa/testData/bbb/C.java", 1, 10, "repository", null);
    assertThat(method.isTest()).isTrue();
  }

  @Test
  public void test_isTest3() {
    final JavaMethod method = new JavaMethod("return", "name", "rawText", "normalizedText", 1,
        0, "aaa/bbb/C.java", 1, 10, "repository", null);
    assertThat(method.isTest()).isFalse();
  }
}
