package iv.data;

import java.util.ArrayList;
import java.util.List;

public class JavaMethod {

  public final String returnType;
  public final String name;
  public final String text;
  public final String path;
  public final int startLine;
  public final int endLine;
  public final String repository;
  public final String commit;
  public final int id;
  private final List<String> parameters;

  public JavaMethod(final String returnType, final String name, final String text,
      final String path, final int startLine, final int endLine, final String repository,
      final String commit) {
    this(returnType, name, text, path, startLine, endLine, repository, commit, -1);
  }

  public JavaMethod(final String returnType, final String name, final String text,
      final String path, final int startLine, final int endLine, final String repository,
      final String commit, final int id) {
    this.returnType = returnType;
    this.parameters = new ArrayList<>();
    this.name = name;
    this.text = text;
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
    lines.add("public class " + className + " {");
    lines.add("");

    lines.add("    // repository: " + repository);
    lines.add("    // commit: " + commit);
    lines.add("    // path: " + path);
    lines.add("    // lines: " + startLine + " to " + endLine);
    lines.add("    // permalink: " + getPermalink());
    for (final String line : text.split(System.lineSeparator())) {
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
}
