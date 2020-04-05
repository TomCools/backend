package life.catalogue.importer;

import life.catalogue.api.model.NameRelation;
import life.catalogue.api.model.ParserConfig;
import life.catalogue.api.model.VerbatimRecord;
import life.catalogue.api.vocab.DataFormat;
import life.catalogue.api.vocab.Issue;
import life.catalogue.api.vocab.NomRelType;
import life.catalogue.dao.ParserConfigDao;
import life.catalogue.db.PgSetupRule;
import life.catalogue.importer.neo.model.NeoUsage;
import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.util.List;

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
      assertEquals("Pardina", s.usage.getName().canonicalNameWithAuthorship());
  
      NeoUsage t = usageByID(s.usage.getParentId());
      assertFalse(t.isSynonym());
      assertEquals("Lynx", t.usage.getName().canonicalNameWithAuthorship());
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
      assertEquals("Aspilota vector Belokobylskij, 2007", u.usage.getName().canonicalNameWithAuthorship());
      assertEquals(NameType.SCIENTIFIC, u.usage.getName().getType());
      assertEquals("Aspilota", u.usage.getName().getGenus());
      assertEquals("vector", u.usage.getName().getSpecificEpithet());

      VerbatimRecord v = store.getVerbatim(u.getVerbatimKey());
      assertEquals(1, v.getIssues().size());
      assertTrue(v.getIssues().contains(Issue.NAME_MATCH_NONE));
    }
  }

}
