package life.catalogue.command.updatedb;

import life.catalogue.command.CmdTestBase;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class ExecSqlCmdTest extends CmdTestBase {

  public ExecSqlCmdTest() {
    super(new ExecSqlCmd());
  }

  @Test
  public void execute() throws Exception {
    assertTrue(run("execSql", "--prompt", "0", "--sql", "SELECT id, scientific_name FROM name_{KEY} LIMIT 1").isEmpty());
  }

  @Test
  public void managed() throws Exception {
    File f = new File("target/test-classes/exec-test.sql");
    assertTrue(f.exists());
    assertTrue(run("execSql", "--prompt", "0", "--managed", "true", "--sqlfile", f.getAbsolutePath()).isEmpty());
  }
}