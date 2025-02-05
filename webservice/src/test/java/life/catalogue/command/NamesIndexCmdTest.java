package life.catalogue.command;

import life.catalogue.db.PgSetupRule;
import life.catalogue.db.TestDataRule;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class NamesIndexCmdTest extends CmdTestBase {

  @ClassRule
  public static PgSetupRule pgSetupRule = new PgSetupRule();

  @Rule
  public final TestDataRule testDataRule = TestDataRule.apple();

  public NamesIndexCmdTest() {
    super(NamesIndexCmd::new);
  }
  
  @Test
  public void testRebuild() throws Exception {
    assertTrue(run("nidx", "--prompt", "0").isEmpty());
  }

}