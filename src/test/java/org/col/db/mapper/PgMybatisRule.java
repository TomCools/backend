package org.col.db.mapper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jersey.repackaged.com.google.common.base.Throwables;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.col.commands.initdb.InitDbCmd;
import org.col.db.MybatisBundle;
import org.junit.rules.ExternalResource;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * A junit test rule that starts up an {@link EmbeddedPostgres} server together with a
 * {@link HikariDataSource} and stops both at the end.
 * The rule was designed to share the server across all tests of a test class if it runs as
 * a static {@link org.junit.ClassRule}.
 *
 * It can even be used to share the same postgres server across several test classes
 * if it is used in as a {@link org.junit.ClassRule} in a TestSuite.
 */
public class PgMybatisRule extends ExternalResource {
  private static EmbeddedPostgres postgres;
  private static HikariDataSource dataSource;
  private static SqlSession session;
  private boolean startedHere = false;

  // switch this for local testing to false
  //Note: DO NOT COMMIT false or jenkins will fail!
  private static final boolean embeddedPg = true;
  private static final String database = "colplus";
  private static final String user = "markus";
  private static final String password = "species2000";

  public <T> T getMapper(Class<T> mapperClazz) {
    return session.getMapper(mapperClazz);
  }

  public static Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public void commit() {
    session.commit();
  }

  @Override
  protected void before() throws Throwable {
    super.before();
    if (postgres == null) {
      startDb();
      startedHere = true;
      initDb();
      initMyBatis();
    }
  }

  private void startDb() {
    try {
      String url;
      if (embeddedPg) {
        System.out.println("Starting Postgres");
        Instant start = Instant.now();
        postgres = new EmbeddedPostgres(Version.V9_6_3);
        // assigned some free port using local socket 0
        url = postgres.start("localhost", new ServerSocket(0).getLocalPort(), database, user, password);
        System.out.format("Pg startup time: %s ms\n", Duration.between(start, Instant.now()).toMillis());
      } else {
        System.out.println("Use external, local Postgres server on database " + database);
        url = "jdbc:postgresql://localhost/"+database;
      }


      HikariConfig hikari = new HikariConfig();
      hikari.setJdbcUrl(url);
      hikari.setUsername(user);
      hikari.setPassword(password);
      hikari.setMaximumPoolSize(2);
      hikari.setMinimumIdle(1);
      dataSource = new HikariDataSource(hikari);

    } catch (Exception e) {
      System.err.println("Pg startup error: " + e.getMessage());
      e.printStackTrace();

      if (dataSource != null) {
        dataSource.close();
      }
      if (postgres != null) {
        postgres.stop();
      }
      Throwables.propagate(e);
    }
  }

  private void initDb() {
    try (Connection con = dataSource.getConnection()) {
      System.out.println("Init empty database schema\n");
      ScriptRunner runner = new ScriptRunner(con);
      runner.runScript(Resources.getResourceAsReader(InitDbCmd.SCHEMA_FILE));
      con.commit();

    } catch (SQLException | IOException e) {
      Throwables.propagate(e);
    }
  }

  private void initMyBatis() {
    SqlSessionFactory factory = MybatisBundle.configure(dataSource, "test");
    session = factory.openSession();
  }

  @Override
  public void after() {
    if (startedHere) {
      System.out.println("Stopping Postgres");
      dataSource.close();
      if (postgres != null) {
        postgres.stop();
      }
    }
  }

}
