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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import iv.IVConfig;
import iv.data.JavaMethod;

public class JavaMethodDAO {

  static public final String METHODS_SCHEMA = "signature string, " + //
      "name string, " + //
      "rtext blob, " + //
      "ntext blob, " + //
      "size int, " + //
      "branches int, " + //
      "hash blob," + //
      "path string, " + //
      "start int, " + //
      "end int, " + //
      "repo string, " + //
      "revision string, " + //
      "compilable int, " + //
      "tests int, " + //
      "Target_ESTest blob, " + //
      "Target_ESTest_scaffolding blob, " + //
      "groupID int, " + //
      "id integer primary key autoincrement";

  static public final String PAIRS_SCHEMA = "leftMethodID int, " + //
      "rightMethodID int, " + //
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

      // methods テーブルの初期化
      statement.executeUpdate("create table if not exists methods (" + METHODS_SCHEMA + ")");
      statement.executeUpdate(
          "create unique index if not exists sameness on methods (path, start, end, repo, revision)");

      // pairs テーブルの初期化
      statement.executeUpdate("create table if not exists pairs (" + PAIRS_SCHEMA + ")");
      statement.executeUpdate(
          "create unique index if not exists sameness on methods (leftMethodID, rightMethodID)");

      connector.commit();
      statement.close();
    } catch (final ClassNotFoundException | SQLException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  synchronized public void addMethods(final List<JavaMethod> methods) {

    if (null == methods || 0 == methods.size()) {
      return;
    }

    try {
      final PreparedStatement statement = this.connector.prepareStatement(
          "insert into methods(signature, name, rtext, ntext, size, branches, hash, path, start, end, repo, revision, compilable, tests, groupID) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
      for (final JavaMethod method : methods) {
        statement.setString(1, method.getSignatureText());
        statement.setString(2, method.name);
        statement.setBytes(3, method.rawText.getBytes());
        statement.setBytes(4, method.normalizedText.getBytes());
        statement.setInt(5, method.size);
        statement.setInt(6, method.branches);
        statement.setBytes(7, method.getMD5());
        statement.setString(8, method.path);
        statement.setInt(9, method.startLine);
        statement.setInt(10, method.endLine);
        statement.setString(11, method.repository);
        statement.setString(12, method.commit);
        statement.setInt(13, -1);
        statement.setInt(14, -1);
        statement.setInt(15, -1);

        try {
          statement.execute();
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

      connector.commit();
      statement.close();

    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }

  synchronized public void setCompilable(final int id, final boolean compilable) {

    try {
      final PreparedStatement statement = connector.prepareStatement(
          "update methods set compilable = ? where id = ?");
      statement.setBoolean(1, compilable);
      statement.setInt(2, id);
      statement.executeUpdate();
      connector.commit();
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean isCompilable(final String id) {

    try {
      return isCompilable(Integer.parseInt(id));
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  synchronized public boolean isCompilable(final int id) {
    try {
      final PreparedStatement statement = connector.prepareStatement(
          "select compilable from methods where id = ?");
      statement.setInt(1, id);
      final ResultSet results = statement.executeQuery();

      // 該当するIDのメソッドがない場合にはfalseを返す
      if (!results.next()) {
        return false;
      }

      // 該当するIDのメソッドがある場合にはそのcompilableカラムをチェックする
      final int compilable = results.getInt(1);
      return compilable == 1;

    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return false;
  }

  public int getNumberOfTests(final String id) {

    try {
      return getNumberOfTests(Integer.parseInt(id));
    } catch (final NumberFormatException e) {
      return 0;
    }
  }

  synchronized public int getNumberOfTests(final int id) {
    try {
      final PreparedStatement statement = connector.prepareStatement(
          "select tests from methods where id = ?");
      statement.setInt(1, id);
      final ResultSet results = statement.executeQuery();

      // 該当するIDのメソッドがない場合には0を返す
      if (!results.next()) {
        return 0;
      }

      // 該当するIDのメソッドがある場合にはそのtestsカラムの値を返す
      final int tests = results.getInt(1);
      return tests;

    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return 0;
  }

  public boolean exists(final String id) {

    try {
      return exists(Integer.parseInt(id));
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  synchronized public boolean exists(final int id) {
    try {
      final PreparedStatement statement = connector.prepareStatement(
          "select * from methods where id = ?");
      statement.setInt(1, id);
      final ResultSet results = statement.executeQuery();
      return results.next();

    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return false;
  }

  synchronized public void setTests(final int id, final int tests, final String target_ESTest,
      final String target_ESTest_scaffolding) {

    try {
      final PreparedStatement statement = connector.prepareStatement(
          "update methods set tests = ?, Target_ESTest = ?, Target_ESTest_scaffolding = ? where id = ?");
      statement.setInt(1, tests);
      statement.setBytes(2, target_ESTest.getBytes());
      statement.setBytes(3, target_ESTest_scaffolding.getBytes());
      statement.setInt(4, id);
      statement.executeUpdate();
      connector.commit();
    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }

  synchronized public void setGroup(final Set<Integer> methodIDs, final int groupID) {

    try {
      final PreparedStatement statement = connector.prepareStatement(
          "update methods set groupID = ? where id = ?");
      for (final Integer methodID : methodIDs) {
        statement.setInt(1, groupID);
        statement.setInt(2, methodID);
        statement.addBatch();
      }
      statement.executeBatch();
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

    final Set<JavaMethod> methods = new TreeSet<>(new Comparator<JavaMethod>() {
      @Override
      public int compare(final JavaMethod o1, final JavaMethod o2) {
        return o1.normalizedText.compareTo(o2.normalizedText);
      }
    });

    try {
      final Statement statement = connector.createStatement();
      final ResultSet results = statement.executeQuery(
          "select name, rtext, ntext, size, branches, path, start, end, repo, revision, id from methods where signature = \""
              + signature + "\"");
      while (results.next()) {
        final String name = results.getString(1);
        final String rtext = new String(results.getBytes(2), StandardCharsets.UTF_8);
        final String ntext = new String(results.getBytes(3), StandardCharsets.UTF_8);
        final int size = results.getInt(4);
        final int branches = results.getInt(5);
        final String path = results.getString(6);
        final int start = results.getInt(7);
        final int end = results.getInt(8);
        final String repo = results.getString(9);
        final String commit = results.getString(10);
        final int id = results.getInt(11);
        final JavaMethod method = new JavaMethod(signature, name, rtext, ntext, size, branches,
            path, start, end, repo, commit, id);
        methods.add(method);
      }
    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return methods.stream()
        .collect(Collectors.toList());
  }

  synchronized public boolean isDifferentSyntax(final Set<Integer> methodIDs) {

    try {
      final PreparedStatement statement = connector.prepareStatement(
          "select hash from methods where id = ?");
      final Set<String> hashes = new HashSet<>();
      for (Integer methodID : methodIDs) {
        statement.setInt(1, methodID);
        final ResultSet results = statement.executeQuery();
        assert results.next() : "no such method ID.";
        final byte[] hash = results.getBytes(1);
        hashes.add(new String(hash, StandardCharsets.UTF_8));
      }

      return 1 < hashes.size();

    } catch (final SQLException e) {
      e.printStackTrace();
    }

    return false;
  }

  synchronized public void close() {
    try {
      connector.close();
    } catch (final SQLException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  synchronized public void addPair(final int leftMethodID, final int rightMethodID) {

    if (leftMethodID <= 0 || rightMethodID <= 0) {
      return;
    }

    try {
      final PreparedStatement statement = this.connector.prepareStatement(
          "insert into pairs(leftMethodID, rightMethodID) values (?, ?)");
      statement.setInt(1, leftMethodID);
      statement.setInt(2, rightMethodID);

      try {
        statement.execute();
      } catch (final SQLiteException e) {
        final SQLiteErrorCode code = e.getResultCode();
        if (code.name()
            .equals("SQLITE_CONSTRAINT_UNIQUE")) {
          System.err.println(
              "already registered: method pair of " + leftMethodID + " and " + rightMethodID);
        } else {
          e.printStackTrace();
        }
      }

      connector.commit();
      statement.close();

    } catch (final SQLException e) {
      e.printStackTrace();
    }
  }
}

