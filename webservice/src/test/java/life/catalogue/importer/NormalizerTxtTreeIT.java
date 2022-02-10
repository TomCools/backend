package life.catalogue.importer;

import life.catalogue.api.model.ParserConfig;
import life.catalogue.api.model.VerbatimRecord;
import life.catalogue.api.vocab.DataFormat;
import life.catalogue.dao.ParserConfigDao;
import life.catalogue.importer.neo.model.NeoUsage;
import life.catalogue.importer.neo.model.RankedUsage;

import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

import java.util.List;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

/**
 *
 */
public class NormalizerTxtTreeIT extends NormalizerITBase {

  public NormalizerTxtTreeIT() {
    super(DataFormat.TEXT_TREE);
  }
  
  @Test
  public void mammalia() throws Exception {
    normalize(0);
    store.dump();
    try (Transaction tx = store.getNeo().beginTx()) {
      NeoUsage s = usageByID("13");
      assertTrue(s.isSynonym());
      assertEquals("Pardina", s.usage.getName().getLabel());
  
      NeoUsage t = usageByID("12");
      assertFalse(t.isSynonym());
      assertEquals("Lynx", t.usage.getName().getLabel());

      List<RankedUsage> accs = store.accepted(s.node);
      assertEquals(1, accs.size());
      assertEquals(t.node, accs.get(0).usageNode);
      assertEquals(t.usage.getName().getLabel(), accs.get(0).name);
    }
  }

  public static ParserConfig aspilotaCfg(){
    ParserConfig cfg = new ParserConfig();
    cfg.updateID("Aspilota vector", "Belokobylskij, 2007");
    cfg.setGenus("Aspilota");
    cfg.setSpecificEpithet("vector");
    cfg.setCombinationAuthorship(Authorship.yearAuthors("2007", "Belokobylskij"));
    cfg.setRank(Rank.SPECIES);
    cfg.setType(NameType.SCIENTIFIC);
    cfg.setCode(NomCode.ZOOLOGICAL);
    return cfg;
  }

  @Test
  public void aspilota() throws Exception {
    // before we run this we configure the name parser to do better
    // then we check that it really worked and no issues get attached
    ParserConfigDao.addToParser(aspilotaCfg());

    normalize(3);
    store.dump();
    try (Transaction tx = store.getNeo().beginTx()) {
      NeoUsage u = usageByID("8");
      assertFalse(u.isSynonym());
      assertEquals("Aspilota vector Belokobylskij, 2007", u.usage.getName().getLabel());
      assertEquals(NameType.SCIENTIFIC, u.usage.getName().getType());
      assertEquals("Aspilota", u.usage.getName().getGenus());
      assertEquals("vector", u.usage.getName().getSpecificEpithet());

      VerbatimRecord v = store.getVerbatim(u.getVerbatimKey());
      assertEquals(0, v.getIssues().size());
    }
  }

}
