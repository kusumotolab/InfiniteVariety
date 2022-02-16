package iv.data;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.eclipse.jgit.revwalk.RevCommit;

public class JavaMethod {

  public final String returnType;
  public final String name;
  public final String rawText;
  public final String normalizedText;
  public final int size;
  public final String path;
  public final int startLine;
  public final int endLine;
  public final String repository;
  public final String commit;
  public final int id;
  private final List<String> parameters;

  public JavaMethod(final String returnType, final String name, final String rawText,
      final String normalizedText, final int size, final String path, final int startLine,
      final int endLine, final String repository,
      final RevCommit commit) {
    this(returnType, name, rawText, normalizedText, size, path, startLine, endLine,
        repository, null != commit ? commit.getName() : null, -1);
  }

  public JavaMethod(final String returnType, final String name, final String rawText,
      final String normalizedText, final int size, final String path, final int startLine,
      final int endLine, final String repository,
      final String commit, final int id) {
    this.returnType = returnType;
    this.parameters = new ArrayList<>();
    this.name = name;
    this.rawText = rawText;
    this.normalizedText = normalizedText;
    this.size = size;
    this.path = path;
    this.startLine = startLine;
    this.endLine = endLine;
    this.repository = repository;
    this.commit = commit;
    this.id = id;
  }

  public void addParameter(final String parameter) {
    parameters.add(parameter);
  }

  public String getSignatureText() {
    final StringBuilder builder = new StringBuilder();
    builder.append(returnType);
    builder.append("(");
    builder.append(String.join(",", parameters));
    builder.append(")");
    return builder.toString();
  }

  public String getNamedSignatureText() {
    final StringBuilder builder = new StringBuilder();
    builder.append(returnType);
    builder.append(" ");
    builder.append(name);
    builder.append("(");
    builder.append(String.join(", ", parameters));
    builder.append(")");
    return builder.toString();
  }

  public String getClassText(final String className) {
    final List<String> lines = new ArrayList<>();
    lines.add("import java.util.*;");
    lines.add("public class " + className + " {");
    lines.add("");

    if (null != repository) {
      lines.add("    // repository: " + repository);
    }
    if (null != commit) {
      lines.add("    // commit: " + commit);
    }
    lines.add("    // path: " + path);
    lines.add("    // lines: " + startLine + " to " + endLine);
    if (null != repository && null != commit) {
      lines.add("    // permalink: " + getPermalink());
    }
    for (final String line : rawText.split(System.lineSeparator())) {
      lines.add("    " + line.replace(name + "(", "__target__("));
    }

    lines.add("");
    lines.add("}");

    return String.join(System.lineSeparator(), lines);
  }

  private String getPermalink() {
    final StringBuilder builder = new StringBuilder();
    builder.append(getHttpsURL());
    builder.append("/blob/");
    builder.append(commit);
    builder.append("/");
    builder.append(path);
    builder.append("#L");
    builder.append(startLine);
    builder.append("-L");
    builder.append(endLine);
    return builder.toString();
  }

  private String getHttpsURL() {
    return repository.replace(":", "/") // httpsより先にある必要あり
        .replace("git@", "https://")
        .replace(".git", "");
  }

  public byte[] getMD5() {
    final String textForHash = normalizedText.replace(" ", "")
        .replace("\t", "")
        .replace(System.lineSeparator(), "");
    try {
      final MessageDigest md5 = MessageDigest.getInstance("MD5");
      return md5.digest(textForHash.getBytes());
    } catch (final NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return new byte[] {};
  }

  public boolean isTest() {
    return path.toLowerCase()
        .endsWith("test.java") || Stream.of(
            path.split(System.lineSeparator()))
        .anyMatch(l -> l.toLowerCase()
            .endsWith("test") || l.toLowerCase()
            .endsWith("tests") || l.equalsIgnoreCase(
            "tsz") || l.equalsIgnoreCase("testdata"));
  }
}
