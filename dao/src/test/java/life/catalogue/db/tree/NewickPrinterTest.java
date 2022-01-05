package life.catalogue.db.tree;

import life.catalogue.api.model.DSID;
import life.catalogue.common.io.Resources;
import life.catalogue.dao.TaxonCounter;
import life.catalogue.db.PgSetupRule;

import life.catalogue.db.TestDataRule;

import org.apache.commons.io.IOUtils;

import org.gbif.nameparser.api.Rank;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class NewickPrinterTest {

  @ClassRule
  public static PgSetupRule pgSetupRule = new PgSetupRule();

  @Rule
  public final TestDataRule testDataRule = TestDataRule.tree();

  @Test
  public void print() throws IOException {
    Writer writer = new StringWriter();
    TaxonCounter taxonCounter = new TaxonCounter() {
      @Override
      public int count(DSID<String> taxonID, Rank countRank) {
        return 999;
      }
    };
    int count = PrinterFactory.dataset(NewickPrinter.class, TestDataRule.TREE.key, null, false, null, Rank.SPECIES, taxonCounter, PgSetupRule.getSqlSessionFactory(), writer).print();
    assertEquals(20, count);
    System.out.println(writer);
    String expected = IOUtils.toString(Resources.stream("trees/tree.newick"), StandardCharsets.UTF_8);
    assertEquals(expected, writer.toString());

    writer = new StringWriter();
    var printer = PrinterFactory.dataset(NewickPrinter.class, TestDataRule.TREE.key, null, false, null, Rank.SPECIES, taxonCounter, PgSetupRule.getSqlSessionFactory(), writer);
    printer.useExtendedFormat();
    count = printer.print();
    assertEquals(20, count);
    System.out.println(writer);
    expected = IOUtils.toString(Resources.stream("trees/tree.nhx"), StandardCharsets.UTF_8);
    assertEquals(expected, writer.toString());
  }
}