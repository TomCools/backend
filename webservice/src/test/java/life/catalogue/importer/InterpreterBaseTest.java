package life.catalogue.importer;

import life.catalogue.api.model.*;
import life.catalogue.api.vocab.*;
import life.catalogue.coldp.ColdpTerm;
import life.catalogue.dao.ReferenceFactory;
import life.catalogue.importer.neo.NeoDb;
import life.catalogue.importer.neo.ReferenceMapStore;
import life.catalogue.importer.neo.model.NeoUsage;

import org.gbif.nameparser.api.Authorship;
import org.gbif.nameparser.api.NamePart;
import org.gbif.nameparser.api.Rank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InterpreterBaseTest {
  
  @Mock
  ReferenceMapStore refStore;

  @Mock
  NeoDb store;

  IssueContainer issues = new IssueContainer.Simple();
  InterpreterBase ib;

  @Before
  public void init() {
    //MockitoAnnotations.initMocks(this);
    when(store.references()).thenReturn(refStore);
    ib = new InterpreterBase(new DatasetSettings(), new ReferenceFactory(1, refStore, null), store);
  }

  @Test
  public void identifier() throws Exception {
    IssueContainer issues = IssueContainer.simple();

    var resp = ib.interpretIdentifiers("tsn:2134,col:GHS", null, issues);
    assertEquals(List.of(new Identifier("tsn","2134"), new Identifier("col","GHS")), resp);

    resp = ib.interpretIdentifiers(" tsn:2134, COL:GHS", null, issues);
    assertEquals(List.of(new Identifier("tsn","2134"), new Identifier("col","GHS")), resp);

    resp = ib.interpretIdentifiers(" tsn:2134\\,COL:GHS", null, issues);
    assertEquals(List.of(new Identifier("tsn", "2134\\,COL:GHS")), resp);

    resp = ib.interpretIdentifiers("https://species.wikimedia.org/wiki/Poa_annua", null, issues);
    assertEquals(List.of(new Identifier("https", "//species.wikimedia.org/wiki/Poa_annua")), resp);

    assertFalse(issues.hasIssues());

    resp = ib.interpretIdentifiers("Poa_annua", null, issues);
    assertEquals(List.of(new Identifier(Identifier.Scope.LOCAL, "Poa_annua")), resp);
    assertTrue(issues.hasIssues());
    assertTrue(issues.hasIssue(Issue.IDENTIFIER_WITHOUT_SCOPE));

    issues.clear();
    resp = ib.interpretIdentifiers("wfo-0001057524", Identifier.Scope.WFO, issues);
    assertEquals(List.of(new Identifier(Identifier.Scope.WFO, "wfo-0001057524")), resp);
    assertFalse(issues.hasIssues());
  }

  @Test
  public void secRefPattern() throws Exception {
    assertSecRef("sensu Busch, 1930", "Busch, 1930");
    assertSecRef("Miller sensu Busch, 1930", "Busch, 1930");
    assertNoSecRef("s.l.");
    assertNoSecRef("s.str.");
    assertNoSecRef("sensu latu");
    assertNoSecRef("sensu lato");
    assertNoSecRef("sensu strict");
    assertNoSecRef("sensu stricto");

    assertNoSecRef("sensu auct.,  non (L.)Mart.");
    assertNoSecRef("sensu auct.,  non Durazz.");
    assertNoSecRef("sensu auctorum");
    assertNoSecRef("sensu auct. non Whittaker");
    assertNoSecRef("auct. nec Whittaker");
    assertNoSecRef("Jurtzev, p.p.");

    assertSecRef("sensu Turcz., p.p.", "Turcz., p.p.");
    assertSecRef("auct. Jurtzev, p.p.", "Jurtzev, p.p.");
    assertSecRef("auct. Whittaker 1981", "Whittaker 1981");
  }

  void assertSecRef(String authorship, String expected) {
    Matcher m = InterpreterBase.SEC_REF.matcher(authorship);
    assertTrue(m.find());
    assertEquals(expected, m.group(2));
  }

  void assertNoSecRef(String authorship) {
    assertFalse(InterpreterBase.SEC_REF.matcher(authorship).find());
  }

  @Test
  public void interpretName() throws Exception {
    VerbatimRecord v = new VerbatimRecord();
    Optional<ParsedNameUsage> pnu;
    Name n;

    pnu = ib.interpretName(true, "1", null, "Cerastium ligusticum subsp. granulatum", "(Huter et al.) P. D. Sell & Whitehead",
      null, null, null, null, null, null, null, null, null, null, "tpl:234567", v);
    assertNull(pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Cerastium ligusticum subsp. granulatum", n.getScientificName());
    assertEquals("(Huter et al.) P. D. Sell & Whitehead", n.getAuthorship());
    assertNull(n.getNomenclaturalNote());
    assertEquals(Rank.SUBSPECIES, n.getRank());
    assertNull(n.getUninomial());
    assertEquals("Cerastium", n.getGenus());
    assertNull(n.getInfragenericEpithet());
    assertEquals("ligusticum", n.getSpecificEpithet());
    assertEquals("granulatum", n.getInfraspecificEpithet());
    assertEquals("(Huter et al.) P. D. Sell & Whitehead", n.getAuthorship());
    assertNull(n.getBasionymAuthorship().getYear());
    assertEquals("Huter", n.getBasionymAuthorship().getAuthors().get(0));
    assertEquals("al.", n.getBasionymAuthorship().getAuthors().get(1));
    assertEquals("P.D.Sell", n.getCombinationAuthorship().getAuthors().get(0));
    assertEquals("Whitehead", n.getCombinationAuthorship().getAuthors().get(1));
    assertEquals(List.of(new Identifier("tpl", "234567")), n.getIdentifier());

    pnu = ib.interpretName(true, "1", "species", "Picea arlba", "Mill. and Desbrochers de Loges, 1881",
      null, "Abies", null, "alba", null, null, null, null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertEquals("Mill. & Desbrochers de Loges, 1881", n.getAuthorship());
    assertEquals(List.of("Mill.", "Desbrochers de Loges"), n.getCombinationAuthorship().getAuthors());
    assertEquals("1881", n.getCombinationAuthorship().getYear());

    // if atoms are given they take precedence over the full name
    pnu = ib.interpretName(true, "1", "species", "Picea arlba Mill. 2121", "",
      null, "Abies", null, "alba", null, null, null, null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertNull(n.getAuthorship());
    assertTrue(n.getCombinationAuthorship().isEmpty());
    assertNull(n.getCombinationAuthorship().getYear());

    // if no authorahip is given it needs to be rebuild
    pnu = ib.interpretName(true, "1", "species", "Abies alba Mill. and Desbrochers de Loges, 1881", "",
      "", "", null, null, null, null, null, null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertEquals("Mill. & Desbrochers de Loges, 1881", n.getAuthorship());
    assertEquals(List.of("Mill.", "Desbrochers de Loges"), n.getCombinationAuthorship().getAuthors());
    assertEquals("1881", n.getCombinationAuthorship().getYear());

    // if no authorahip is given it needs to be rebuild
    pnu = ib.interpretName(true, "1", "species", "Abies alba Mill. and Desbrochers de Loges, 1881", "",
      "", "", null, null, null, null, null, null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertEquals("Mill. & Desbrochers de Loges, 1881", n.getAuthorship());
    assertEquals(List.of("Mill.", "Desbrochers de Loges"), n.getCombinationAuthorship().getAuthors());
    assertEquals("1881", n.getCombinationAuthorship().getYear());

    // exclude taxon notes from authorship
    pnu = ib.interpretName(true, "1", "species", "Abies alba Mill. and Desbrochers de Loges, 1881 sensu Döring 1999", "",
      null, "", null, null, null, null, null, null, null, null, null, v);
    assertEquals("sensu Döring 1999", pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertEquals("Mill. & Desbrochers de Loges, 1881", n.getAuthorship());
    assertEquals(List.of("Mill.", "Desbrochers de Loges"), n.getCombinationAuthorship().getAuthors());
    assertEquals("1881", n.getCombinationAuthorship().getYear());

    pnu = ib.interpretName(true, "1", "species", "Abies alba", "Mill. and Desbrochers de Loges, 1881 sensu Döring 1999",
      null, "", null, null, null, null, null, null, null, null, null, v);
    assertEquals("sensu Döring 1999", pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertEquals("Mill. & Desbrochers de Loges, 1881", n.getAuthorship());
    assertEquals(List.of("Mill.", "Desbrochers de Loges"), n.getCombinationAuthorship().getAuthors());
    assertEquals("1881", n.getCombinationAuthorship().getYear());

    pnu = ib.interpretName(true, "1", "family", "", "Miller",
      "Asteraceae", "", null, null, null, null, null, null, null, null, null, v);
    assertNull(pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Asteraceae", n.getScientificName());
    assertEquals("Asteraceae", n.getUninomial());
    assertEquals(Rank.FAMILY, n.getRank());
    assertNull(n.getGenus());
    assertNull(n.getSpecificEpithet());
    assertEquals("Miller", n.getAuthorship());
    assertEquals(List.of("Miller"), n.getCombinationAuthorship().getAuthors());
    assertNull(n.getCombinationAuthorship().getYear());

    // https://github.com/CatalogueOfLife/backend/issues/788
    pnu = ib.interpretName(true, "CIP-82", "species", "Lutzomyia (Helcocyrtomyia) osornoi", "(Ristorcelli & Van ty, 1941)",
      null, "Lutzomyia", "Helcocyrtomyia", "osornoi", null, null, null, null, null, null, null, v);
    assertNull(pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Lutzomyia (Helcocyrtomyia) osornoi", n.getScientificName());
    assertNull(n.getUninomial());
    assertEquals(Rank.SPECIES, n.getRank());
    assertEquals("Lutzomyia", n.getGenus());
    assertEquals("Helcocyrtomyia", n.getInfragenericEpithet());
    assertEquals("osornoi", n.getSpecificEpithet());
    assertEquals("(Ristorcelli & Van ty, 1941)", n.getAuthorship());
    assertTrue(n.getCombinationAuthorship().isEmpty());
    assertEquals("1941", n.getBasionymAuthorship().getYear());
    assertEquals("Ristorcelli", n.getBasionymAuthorship().getAuthors().get(0));
    assertEquals("Van ty", n.getBasionymAuthorship().getAuthors().get(1));
    assertTrue(n.getBasionymAuthorship().getExAuthors().isEmpty());

    // https://github.com/CatalogueOfLife/backend/issues/788
    pnu = ib.interpretName(true, "1", "superfamily", "Eucnidoideae ined.", "ined.",
      null, null, null, null, null, null, null, null, null, null, null, v);
    assertNull(pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Eucnidoideae", n.getScientificName());
    assertEquals("ined.", n.getAuthorship());
    assertEquals("ined.", n.getNomenclaturalNote());
    assertEquals(Rank.SUPERFAMILY, n.getRank());
    assertEquals("Eucnidoideae", n.getUninomial());
    assertNull(n.getGenus());
    assertNull(n.getInfragenericEpithet());
    assertNull(n.getSpecificEpithet());
    assertTrue(n.getCombinationAuthorship().isEmpty());
    assertTrue(n.getCombinationAuthorship().isEmpty());
    assertTrue(n.getBasionymAuthorship().isEmpty());

    // https://github.com/CatalogueOfLife/backend/issues/788
    pnu = ib.interpretName(true, "1", null, "Cerastium ligusticum subsp. granulatum", "(Huter et al.) P. D. Sell & Whitehead",
      null, null, null, null, null, null, null, null, null, null, null, v);
    assertNull(pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertEquals("Cerastium ligusticum subsp. granulatum", n.getScientificName());
    assertEquals("(Huter et al.) P. D. Sell & Whitehead", n.getAuthorship());
    assertNull(n.getNomenclaturalNote());
    assertEquals(Rank.SUBSPECIES, n.getRank());
    assertNull(n.getUninomial());
    assertEquals("Cerastium", n.getGenus());
    assertNull(n.getInfragenericEpithet());
    assertEquals("ligusticum", n.getSpecificEpithet());
    assertEquals("granulatum", n.getInfraspecificEpithet());
    assertEquals("(Huter et al.) P. D. Sell & Whitehead", n.getAuthorship());
    assertNull(n.getBasionymAuthorship().getYear());
    assertEquals("Huter", n.getBasionymAuthorship().getAuthors().get(0));
    assertEquals("al.", n.getBasionymAuthorship().getAuthors().get(1));
    assertEquals("P.D.Sell", n.getCombinationAuthorship().getAuthors().get(0));
    assertEquals("Whitehead", n.getCombinationAuthorship().getAuthors().get(1));

    // Odonata INCONSISTENT_AUTHORSHIP
    v = new VerbatimRecord();
    pnu = ib.interpretName(true, "957", "species", "Boyeria vinosa (Say, 1840)", "(Say, 1840)",
      null, "Boyeria", null, "vinosa", null, null, null, null, null, null, null, v);
    assertNull(pnu.get().getTaxonomicNote());
    n = pnu.get().getName();
    assertFalse(v.hasIssues());

    // explicit unranked should stay: https://github.com/CatalogueOfLife/backend/issues/1136
    pnu = ib.interpretName(true, "1", "no rank", "cellular organisms", null,
      null, null, null, null, null, null, null, null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Cellular organisms", n.getScientificName());
    assertEquals(Rank.UNRANKED, n.getRank());
    assertEquals("Cellular", n.getGenus());
    assertEquals("organisms", n.getSpecificEpithet());
    assertNull(n.getAuthorship());
    assertTrue(n.getCombinationAuthorship().isEmpty());
    assertNull(n.getCombinationAuthorship().getYear());

    // daggers should be removed from name parts, not just entire names
    // https://github.com/CatalogueOfLife/data/issues/417
    pnu = ib.interpretName(true, "1", "species", null, "Hamilton, 1990",
      null, "Acixiites†", null, "costalis", null, null, null, "original combination, valid: Yes", null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Acixiites costalis", n.getScientificName());
    assertEquals("Acixiites", n.getGenus());
    assertEquals("costalis", n.getSpecificEpithet());
    assertEquals("Hamilton, 1990", n.getAuthorship());
    assertEquals(Authorship.yearAuthors("1990", "Hamilton"), n.getCombinationAuthorship());
    assertTrue(n.getBasionymAuthorship().isEmpty());

    pnu = ib.interpretName(true, "1", "species", null, "Hamilton, 1990",
      null, "Acixiites", null, "costalis †", null, null, null, null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Acixiites costalis", n.getScientificName());
    assertEquals("Acixiites", n.getGenus());
    assertEquals("costalis", n.getSpecificEpithet());
    assertEquals("Hamilton, 1990", n.getAuthorship());
    assertEquals(Authorship.yearAuthors("1990", "Hamilton"), n.getCombinationAuthorship());
    assertTrue(n.getBasionymAuthorship().isEmpty());

    pnu = ib.interpretName(true, "1", null, "Leucophyta R. Br.", null, null, null, null, null, null, null, "botanical", null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Leucophyta", n.getScientificName());
    assertEquals("R.Br.", n.getAuthorship());
    assertEquals("Leucophyta", n.getUninomial());
    assertNull(n.getGenus());
    assertNull(n.getSpecificEpithet());
    assertEquals(Rank.UNRANKED, n.getRank());

    // https://github.com/CatalogueOfLife/testing/issues/195
    ib.settings.enable(Setting.EPITHET_ADD_HYPHEN);
    pnu = ib.interpretName(true, "1", null, null, null, null, "Cosmopterix", null, "sancti vincentii", null, null, "botanical", null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Cosmopterix sancti-vincentii", n.getScientificName());
    assertNull(n.getAuthorship());
    assertNull(n.getUninomial());
    assertEquals("Cosmopterix", n.getGenus());
    assertEquals("sancti-vincentii", n.getSpecificEpithet());

    // https://github.com/CatalogueOfLife/data/issues/431
    ib.settings.disable(Setting.EPITHET_ADD_HYPHEN);
    pnu = ib.interpretName(true, "1", null, "Asplenium × mitsutae Viane & Reichst.", "Viane & Reichst.", null, "Asplenium", null, "× mitsutae", null, null, "botanical", null, null, null, null, v);
    n = pnu.get().getName();
    assertEquals("Asplenium × mitsutae", n.getScientificName());
    assertEquals("Viane & Reichst.", n.getAuthorship());
    assertNull(n.getUninomial());
    assertEquals("Asplenium", n.getGenus());
    assertEquals("mitsutae", n.getSpecificEpithet());
    assertEquals(NamePart.SPECIFIC, n.getNotho());
  }

  @Test
  public void interpretUsage() throws Exception {
    VerbatimRecord v = new VerbatimRecord();

    Name n = new Name();
    n.setGenus("Abies");
    n.setSpecificEpithet("alba");
    n.setAuthorship("Miller 1876");
    n.setCombinationAuthorship(Authorship.yearAuthors("1876", "Miller"));
    n.rebuildScientificName();

    ParsedNameUsage pnu = new ParsedNameUsage(n, true, "sensu Döring 1999", "Döring 1999. Travels through the Middle East");

    NeoUsage u = ib.interpretUsage(ColdpTerm.ID, pnu, ColdpTerm.status, TaxonomicStatus.ACCEPTED, v, Collections.emptyMap());

    assertTrue(u.usage.isTaxon());
    Taxon t = u.asTaxon();

    assertTrue(t.isExtinct());
    assertNull(t.getNamePhrase());
    // stubbed refstore does assign ids
    //assertNotNull(t.getAccordingToId());

    assertNull(t.getNamePhrase());

    n = t.getName();
    assertEquals("Abies alba", n.getScientificName());
    assertEquals("Abies", n.getGenus());
    assertEquals("alba", n.getSpecificEpithet());
    assertEquals("Miller 1876", n.getAuthorship());
  }

  @Test
  public void yearParser() throws Exception {
    assertEquals((Integer) 1678, InterpreterBase.parseYear("1678", issues));
    assertFalse(issues.hasIssues());
  
    assertEquals((Integer) 1678, InterpreterBase.parseYear("1678b", issues));
    assertFalse(issues.hasIssues());
  
    assertEquals((Integer) 1678, InterpreterBase.parseYear(" 1678 b", issues));
    assertFalse(issues.hasIssues());
    
    assertEquals((Integer) 999, InterpreterBase.parseYear("999", issues));
    assertTrue(issues.hasIssue(Issue.UNLIKELY_YEAR));
    
    issues.getIssues().clear();
    assertEquals((Integer) 2112, InterpreterBase.parseYear("2112", issues));
    assertTrue(issues.hasIssue(Issue.UNLIKELY_YEAR));
  
    issues.getIssues().clear();
    assertEquals((Integer) 2800, InterpreterBase.parseYear("2800", issues));
    assertTrue(issues.hasIssue(Issue.UNLIKELY_YEAR));

    issues.getIssues().clear();
    assertEquals((Integer) 1980, InterpreterBase.parseYear("198?", issues));
    assertFalse(issues.hasIssues());
  
    issues.getIssues().clear();
    assertNull(InterpreterBase.parseYear("gd2000", issues));
    assertTrue(issues.hasIssue(Issue.UNPARSABLE_YEAR));
  
    issues.getIssues().clear();
    assertNull(InterpreterBase.parseYear("35611", issues));
    assertTrue(issues.hasIssue(Issue.UNPARSABLE_YEAR));
  
    issues.getIssues().clear();
    assertNull(InterpreterBase.parseYear("january", issues));
    assertTrue(issues.hasIssue(Issue.UNPARSABLE_YEAR));
  }

  @Test
  public void createDistributions() throws Exception {
    assertDistributions(Gazetteer.ISO, "DE", "DE");
    assertDistributions(Gazetteer.ISO, "DE,fr,es", "DE", "FR", "ES");
    assertDistributions(Gazetteer.ISO, "az-tar; AZ; ", "AZ", "AZ");
    // ignore unparsable regions
    assertDistributions(Gazetteer.ISO, "Bolivia (Chuquisaca, Cochabamba, Santa Cruz, Tarija), Peru, Colombia (Amazonas, Antioquia, Boyac, Cauca, Choc, Cundinamarca, Huila, La Guajira, Magdalena, Meta, Nario, Putumayo, Risaralda, Santander, Valle), NE-Brazil (Pernambuco, Bahia, Alagoas), WC-Brazil (Goias, Distrito Federal, Mato Grosso do Sul), SE-Brazil (Minas Gerais, Espirito Santo, Sao Paulo, Rio de Janeiro), Ecuador, Galapagos, Mexico (Hidalgo, Michoacan, Oaxaca, Tamaulipas, Veracruz), Venezuela (Tachira), Argentina (Buenos Aires, Chaco, Chubut, Cordoba, Corrientes, Entre Rios, Formosa, Jujuy, Misiones,Rio Negro, Salta, Santa Fe, Tucuman), S-Brazil (Parana, Rio Grande do Sul, Santa Catarina), Paraguay (Caazapa, Central, Cordillera, Guaira, Paraguari), Uruguay (Artigas, Canelones, Cerro Largo, Colonia, Durazno, Flores Prov., Florida Prov., Lavalleja, Maldonado, Montevideo, Paysandu, Rio Negro, Rivera, Rocha, Salto, San Jose, Soriano, Tacuarembo, Treinta y Tres), Costa Rica (I)",
            Country.BOLIVIA, Country.PERU, Country.COLOMBIA, Country.ECUADOR, Country.MEXICO,
            Country.VENEZUELA, Country.ARGENTINA, Country.PARAGUAY, Country.URUGUAY, Country.COSTA_RICA);
    // MRGID URLs, see https://github.com/CatalogueOfLife/data/issues/483
    assertDistributions(Gazetteer.MRGID, "http://marineregions.org/mrgid/48213", "48213");
    assertDistributions(Gazetteer.MRGID, "https://marineregions.org/mrgid/48213", "48213");
  }

  private void assertDistributions(Gazetteer std, String loc, Country... expected) {
    String[] exp = Arrays.stream(expected).map(Country::getIso2LetterCode).toArray(String[]::new);
    assertDistributions(std, loc, exp);
  }

  private void assertDistributions(Gazetteer std, String loc, String... expectedIDs) {
    List<Distribution> dis = InterpreterBase.createDistributions(std, loc, "present", new VerbatimRecord(), new BiConsumer<Distribution, VerbatimRecord>() {
      @Override
      public void accept(Distribution distribution, VerbatimRecord verbatimRecord) {
        // dont do anything
      }
    });

    int counter = 0;
    for (Distribution d : dis) {
      assertEquals(std, d.getArea().getGazetteer());
      assertEquals(expectedIDs[counter++], d.getArea().getId());
    }
  }
}
