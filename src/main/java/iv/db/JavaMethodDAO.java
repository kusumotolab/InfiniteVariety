package iv.db;

import java.nio.charset.StandardCharsets;
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
      "text blob, " + //
      "path string, " + //
      "start int, " + //
      "end int, " + //
      "repo string, " + //
      "revision string, " + //
      "compilable int, " + //
      "Target_ESTest blob, " + //
      "Target_ESTest_scaffolding blob, " + //
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
          "insert into methods(signature, name, text, path, start, end, repo, revision, compilable) values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
      for (final JavaMethod method : methods) {
        statement.setString(1, method.getSignatureText());
        statement.setString(2, method.name);
        statement.setBytes(3, method.text.getBytes());
        statement.setString(4, method.path);
        statement.setInt(5, method.startLine);
        statement.setInt(6, method.endLine);
        statement.setString(7, method.repository);
        statement.setString(8, method.commit);
        statement.setInt(9, -1);
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

  synchronized public void setCompilable(final int id, final boolean compilable) {

    try {
      System.out.println("setCompilable: " + id);
      final PreparedStatement statement = connector.prepareStatement("update methods set compilable = ? where id = ?");
      statement.setBoolean(1, compilable);
      statement.setInt(2, id);
      statement.executeUpdate();
      connector.commit();
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean isCompilable(final String id){

    try{
      return isCompilable(Integer.parseInt(id));
    }catch(final NumberFormatException e){
      return false;
    }
  }

  synchronized public boolean isCompilable(final int id){
    try {
      final PreparedStatement statement = connector.prepareStatement("select compilable from methods where id = ?");
      statement.setInt(1, id);
      final ResultSet results = statement.executeQuery();

      // 該当するIDのメソッドがない場合にはfalseを返す
      if(!results.next()){
        return false;
      }

      // 該当するIDのメソッドがある場合にはそのcompilableカラムをチェックする
      final int compilable = results.getInt(1);
      return compilable == 1;

    }catch(final SQLException e){
      e.printStackTrace();
    }

    return false;
  }



  synchronized public void setTests(final int id, final String target_ESTest,
      final String target_ESTest_scaffolding) {

    try {
      final PreparedStatement statement = connector.prepareStatement(
          "update methods set Target_ESTest = ?, Target_ESTest_scaffolding = ? where id = ?");
      statement.setBytes(1, target_ESTest.getBytes());
      statement.setBytes(2, target_ESTest_scaffolding.getBytes());
      statement.setInt(3, id);
      statement.executeUpdate();
      connector.commit();
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
        final String text = new String(results.getBytes(2), StandardCharsets.UTF_8);
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

