package iv.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import iv.IVConfig;
import iv.data.JavaMethod;

public class JavaMethodDAO {

  static public final String METHODS_SCHEMA = "signature string, " + //
      "name string, " + //
      "text string, " + //
      "path string, " + //
      "start int, " + //
      "end int, " + //
      "repo string, " + //
      "revision string, " + //
      "id integer primary key autoincrement";

  static public JavaMethodDAO SINGLETON = new JavaMethodDAO();
  private Connection connector;
  private IVConfig config;

  private JavaMethodDAO() {
  }

  synchronized public void initialize(final IVConfig config) {

    this.config = config;

    try {
      Class.forName("org.sqlite.JDBC");
      final Path dbPath = config.getDatabase();
      connector = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
      connector.setAutoCommit(false);

      final Statement statement = connector.createStatement();
      statement.executeUpdate("create table if not exists methods (" + METHODS_SCHEMA + ")");
      statement.executeUpdate(
          "create unique index if not exists sameness on methods (path, start, end, repo, revision)");
      connector.commit();
      statement.close();
    } catch (final ClassNotFoundException | SQLException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  synchronized public void addMethods(final List<JavaMethod> methods) {

    try {
      final PreparedStatement statement = this.connector.prepareStatement(
          "insert into methods(signature, name, text, path, start, end, repo, revision) values (?, ?, ?, ?, ?, ?, ?, ?)");
      for (final JavaMethod method : methods) {
        statement.setString(1, method.getSignatureText());
        statement.setString(2, method.name);
        statement.setString(3, method.text);
        statement.setString(4, method.path);
        statement.setInt(5, method.startLine);
        statement.setInt(6, method.endLine);
        statement.setString(7, method.repository);
        statement.setString(8, method.commit);
        try {
          statement.execute();
          connector.commit();
        } catch (final SQLiteException e) {
          final SQLiteErrorCode code = e.getResultCode();
          if (code.name()
              .equals("SQLITE_CONSTRAINT_UNIQUE")) {
            System.err.println(
                "already registered: \"" + method.getNamedSignatureText() + "\" in " + method.path);
          } else {
            e.printStackTrace();
          }
        }
      }

      statement.close();

    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }

  synchronized public List<String> getSignatures() {

    final List<String> signatures = new ArrayList<>();

    try {
      final Statement statement = connector.createStatement();
      final ResultSet results = statement.executeQuery("select distinct(signature) from methods");
      while (results.next()) {
        final String signature = results.getString(1);
        signatures.add(signature);
      }
    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return signatures;
  }

  synchronized public List<JavaMethod> getMethods(final String signature) {

    final List<JavaMethod> methods = new ArrayList<>();

    try {
      final Statement statement = connector.createStatement();
      final ResultSet results = statement.executeQuery(
          "select name, text, path, start, end, repo, revision, id from methods where signature = \""
              + signature + "\"");
      while (results.next()) {
        final String name = results.getString(1);
        final String text = results.getString(2);
        final String path = results.getString(3);
        final int start = results.getInt(4);
        final int end = results.getInt(5);
        final String repo = results.getString(6);
        final String commit = results.getString(7);
        final int id = results.getInt(8);
        final JavaMethod method = new JavaMethod(signature, name, text, path, start, end, repo,
            commit, id);
        methods.add(method);
      }
    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return methods;
  }


  synchronized public void close() {
    try {
      connector.close();
    } catch (final SQLException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }
}

