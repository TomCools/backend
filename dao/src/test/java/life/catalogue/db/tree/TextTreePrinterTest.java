package life.catalogue.db.tree;

import life.catalogue.api.model.DSID;
import life.catalogue.api.model.TreeTraversalParameter;
import life.catalogue.common.io.Resources;
import life.catalogue.common.io.UTF8IoUtils;
import life.catalogue.dao.TaxonCounter;
import life.catalogue.db.PgSetupRule;
import life.catalogue.db.SqlSessionFactoryRule;
import life.catalogue.db.TestDataRule;

import org.gbif.nameparser.api.Rank;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TextTreePrinterTest {
  
  @ClassRule
  public static PgSetupRule pgSetupRule = new PgSetupRule();
  
  @Rule
  public final TestDataRule testDataRule = TestDataRule.tree2();

  @Test
  public void print() throws IOException {
    Writer writer = new StringWriter();
    int count = PrinterFactory.dataset(TextTreePrinter.class, TestDataRule.TREE.key, SqlSessionFactoryRule.getSqlSessionFactory(), writer).print();
    assertEquals(25, count);
    String expected = UTF8IoUtils.readString(Resources.stream("trees/tree2.tree"));
    assertEquals(expected, writer.toString());
  }

  @Test
  public void printWithCounts() throws IOException {
    Writer writer = new StringWriter();
    AtomicInteger cnt = new AtomicInteger(1);
    TaxonCounter counter = new TaxonCounter() {
      @Override
      public int count(DSID<String> taxonID, Rank countRank) {
        return cnt.getAndIncrement();
      }
    };
    var p = PrinterFactory.dataset(TextTreePrinter.class, TreeTraversalParameter.datasetNoSynonyms(TestDataRule.TREE.key), Set.of(Rank.FAMILY, Rank.GENUS), Rank.SPECIES, counter, SqlSessionFactoryRule.getSqlSessionFactory(), writer);
    p.showIDs();
    int count = p.print();
    System.out.println(writer);
    assertEquals(5, count);
    String expected = UTF8IoUtils.readString(Resources.stream("trees/treeWithCounts.tree"));
    assertEquals(expected, writer.toString());

    // test with extinct
    for (boolean extinct : List.of(true, false)) {
      writer = new StringWriter();
      cnt.set(1);
      var ttp = TreeTraversalParameter.dataset(TestDataRule.TREE.key);
      ttp.setSynonyms(false);
      ttp.setExtinct(extinct);
      p = PrinterFactory.dataset(TextTreePrinter.class, ttp, Set.of(Rank.FAMILY, Rank.GENUS), Rank.SPECIES, counter, SqlSessionFactoryRule.getSqlSessionFactory(), writer);
      p.showIDs();
      count = p.print();
      System.out.println(writer);
      assertEquals(0, count);
    }
  }

}