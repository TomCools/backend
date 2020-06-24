package life.catalogue.es.nu.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.elasticsearch.client.RestClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import life.catalogue.api.TestEntityGenerator;
import life.catalogue.api.model.BareName;
import life.catalogue.api.model.Name;
import life.catalogue.api.model.Page;
import life.catalogue.api.model.ResultPage;
import life.catalogue.api.model.Taxon;
import life.catalogue.api.model.VernacularName;
import life.catalogue.api.search.NameUsageSearchParameter;
import life.catalogue.api.search.NameUsageSearchRequest;
import life.catalogue.api.search.NameUsageSearchResponse;
import life.catalogue.api.search.NameUsageWrapper;
import life.catalogue.api.vocab.Issue;
import life.catalogue.es.EsReadTestBase;
import life.catalogue.es.nu.NameUsageWrapperConverter;
import static org.junit.Assert.assertEquals;
import static life.catalogue.es.EsUtil.insert;
import static life.catalogue.es.EsUtil.refreshIndex;

public class NameUsageSearchServiceTest extends EsReadTestBase {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(NameUsageSearchServiceTest.class);

  private static RestClient client;
  private static NameUsageSearchServiceEs svc;
  private static NameUsageWrapperConverter CONVERTER = new NameUsageWrapperConverter();

  @BeforeClass
  public static void init() {
    client = esSetupRule.getClient();
    svc = new NameUsageSearchServiceEs(esSetupRule.getEsConfig().nameUsage.name, esSetupRule.getClient());
  }

  @Before
  public void before() {
    destroyAndCreateIndex();
  }

  @Test
  public void testQuery1() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Define search
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.setHighlight(false);
    nsr.addFilter(NameUsageSearchParameter.ISSUE, Issue.ACCEPTED_NAME_MISSING);

    // Match
    NameUsageWrapper nuw1 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw1.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING));
    insert(client, indexName(), converter.toDocument(nuw1));

    // Match
    NameUsageWrapper nuw2 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw2.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.ACCORDING_TO_DATE_INVALID));
    insert(client, indexName(), converter.toDocument(nuw2));

    // Match
    NameUsageWrapper nuw3 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw3.setIssues(EnumSet.allOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw3));

    // No match
    NameUsageWrapper nuw4 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw4.setIssues(null);
    insert(client, indexName(), converter.toDocument(nuw4));

    // No match
    NameUsageWrapper nuw5 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw5.setIssues(EnumSet.of(Issue.CITATION_UNPARSED));
    insert(client, indexName(), converter.toDocument(nuw5));

    // No match
    NameUsageWrapper nuw6 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw6.setIssues(EnumSet.of(Issue.CITATION_UNPARSED, Issue.BASIONYM_ID_INVALID));
    insert(client, indexName(), converter.toDocument(nuw6));

    // No match
    NameUsageWrapper nuw7 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw7.setIssues(EnumSet.noneOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw7));

    refreshIndex(client, indexName());

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(3, result.getResult().size());
  }

  @Test
  public void testQuery2() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Find all documents with an issue of either ACCEPTED_NAME_MISSING or ACCORDING_TO_DATE_INVALID
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.addFilter(NameUsageSearchParameter.ISSUE, EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.ACCORDING_TO_DATE_INVALID));

    // Match
    NameUsageWrapper nuw1 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw1.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING));
    insert(client, indexName(), converter.toDocument(nuw1));

    // Match
    NameUsageWrapper nuw2 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw2.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.ACCORDING_TO_DATE_INVALID));
    insert(client, indexName(), converter.toDocument(nuw2));

    // Match
    NameUsageWrapper nuw3 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw3.setIssues(EnumSet.allOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw3));

    // No match
    NameUsageWrapper nuw4 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw4.setIssues(null);
    insert(client, indexName(), converter.toDocument(nuw4));

    // No match
    NameUsageWrapper nuw5 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw5.setIssues(EnumSet.of(Issue.CITATION_UNPARSED));
    insert(client, indexName(), converter.toDocument(nuw5));

    // No match
    NameUsageWrapper nuw6 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw6.setIssues(EnumSet.of(Issue.CITATION_UNPARSED, Issue.BASIONYM_ID_INVALID));
    insert(client, indexName(), converter.toDocument(nuw6));

    // No match
    NameUsageWrapper nuw7 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw7.setIssues(EnumSet.noneOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw7));

    // No match
    NameUsageWrapper nuw8 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw8.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.DOUBTFUL_NAME));
    insert(client, indexName(), converter.toDocument(nuw8));

    refreshIndex(client, indexName());

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(4, result.getResult().size());
  }

  @Test
  public void testQuery3() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Find all documents with an issue of any of ACCEPTED_NAME_MISSING, ACCORDING_TO_DATE_INVALID, BASIONYM_ID_INVALID
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.setHighlight(false);
    nsr.addFilter(NameUsageSearchParameter.ISSUE,
        Issue.ACCEPTED_NAME_MISSING,
        Issue.ACCORDING_TO_DATE_INVALID,
        Issue.BASIONYM_ID_INVALID);

    // Match
    NameUsageWrapper nuw1 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw1.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING));
    insert(client, indexName(), converter.toDocument(nuw1));

    // Match
    NameUsageWrapper nuw2 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw2.setIssues(EnumSet.of(Issue.ACCEPTED_NAME_MISSING, Issue.ACCORDING_TO_DATE_INVALID));
    insert(client, indexName(), converter.toDocument(nuw2));

    // Match
    NameUsageWrapper nuw3 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw3.setIssues(EnumSet.allOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw3));

    // No match
    NameUsageWrapper nuw4 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw4.setIssues(null);
    insert(client, indexName(), converter.toDocument(nuw4));

    // No match
    NameUsageWrapper nuw5 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw5.setIssues(EnumSet.of(Issue.CITATION_UNPARSED));
    insert(client, indexName(), converter.toDocument(nuw5));

    // Match
    NameUsageWrapper nuw6 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw6.setIssues(EnumSet.of(Issue.CITATION_UNPARSED, Issue.BASIONYM_ID_INVALID));
    insert(client, indexName(), converter.toDocument(nuw6));

    // No match
    NameUsageWrapper nuw7 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw7.setIssues(EnumSet.noneOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw7));

    refreshIndex(client, indexName());

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(4, result.getResult().size());
  }


  @Test
  public void testIsNull() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Define search condition
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.setHighlight(false);
    nsr.addFilter(NameUsageSearchParameter.ISSUE, NameUsageSearchRequest.IS_NULL);

    // Match
    NameUsageWrapper nuw1 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw1.setIssues(EnumSet.noneOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw1));
    // No match
    NameUsageWrapper nuw2 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw2.setIssues(EnumSet.allOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw2));
    // Match
    NameUsageWrapper nuw3 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw3.setIssues(null);
    insert(client, indexName(), converter.toDocument(nuw3));
    // No match
    NameUsageWrapper nuw4 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw4.setIssues(EnumSet.of(Issue.CITATION_UNPARSED));
    insert(client, indexName(), converter.toDocument(nuw4));

    refreshIndex(client, indexName());

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(2, result.getResult().size());
  }

  @Test
  public void testIsNotNull() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Define search condition
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.addFilter(NameUsageSearchParameter.ISSUE, NameUsageSearchRequest.IS_NOT_NULL);

    // No match
    NameUsageWrapper nuw1 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw1.setIssues(EnumSet.noneOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw1));
    // Match
    NameUsageWrapper nuw2 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw2.setIssues(EnumSet.allOf(Issue.class));
    insert(client, indexName(), converter.toDocument(nuw2));
    // No match
    NameUsageWrapper nuw3 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw3.setIssues(null);
    insert(client, indexName(), converter.toDocument(nuw3));
    // Match
    NameUsageWrapper nuw4 = TestEntityGenerator.newNameUsageTaxonWrapper();
    nuw4.setIssues(EnumSet.of(Issue.CITATION_UNPARSED));
    insert(client, indexName(), converter.toDocument(nuw4));

    refreshIndex(client, indexName());

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(2, result.getResult().size());
  }

  /**
   * @return id of the new taxon
   */
  private String insertRndTaxon(int datasetKey) throws IOException {
    Taxon t = TestEntityGenerator.newTaxon();
    // taxon id will be pruned by indexing code
    final String id = t.getId();
    t.setDatasetKey(datasetKey);
    t.getName().setDatasetKey(datasetKey);
    NameUsageWrapper nuw = new NameUsageWrapper(t);
    insert(client, indexName(), CONVERTER.toDocument(nuw));
    return id;
  }

  /**
   * Tests for mulitple usage ID queries
   */
  @Test
  public void testUsageIDSearch() throws IOException {
    final int datasetKey = 1000;
    List<String> ids = new ArrayList<>();
    ids.add(insertRndTaxon(datasetKey));
    ids.add(insertRndTaxon(datasetKey));
    ids.add(insertRndTaxon(datasetKey));
    ids.add(insertRndTaxon(datasetKey));
    ids.add(insertRndTaxon(datasetKey));
    ids.add(insertRndTaxon(datasetKey));
    refreshIndex(client, indexName());

    NameUsageSearchRequest nsr = new NameUsageSearchRequest();

    nsr.addFilter(NameUsageSearchParameter.DATASET_KEY, datasetKey);
    nsr.addFilter(NameUsageSearchParameter.USAGE_ID, ids.get(0));
    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());
    assertEquals(1, result.getResult().size());
    assertEquals(ids.get(0), result.getResult().get(0).getUsage().getId());
    assertEquals(datasetKey, (int) result.getResult().get(0).getUsage().getDatasetKey());

    nsr.addFilter(NameUsageSearchParameter.USAGE_ID, ids.get(2));
    nsr.addFilter(NameUsageSearchParameter.USAGE_ID, ids.get(4));
    result = svc.search(indexName(), nsr, new Page());
    assertEquals(3, result.getResult().size());
  }

  @Test
  public void testNameFieldsQuery1() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Find all documents where the uninomial field is not empty
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.setHighlight(false);
    nsr.addFilter(NameUsageSearchParameter.FIELD, "uninomial");

    // Match
    Name n = new Name();
    n.setUninomial("laridae");
    BareName bn = new BareName(n);
    NameUsageWrapper nuw1 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw1));

    // Match
    n = new Name();
    n.setUninomial("parus");
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw2 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw2));

    // No match
    n = new Name();
    n.setUninomial(null);
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw3 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw3));

    // Match
    n = new Name();
    n.setUninomial("parus");
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw4 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw4));

    refreshIndex(client, indexName());

    List<NameUsageWrapper> expected = Arrays.asList(nuw1, nuw2, nuw4);

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(expected, result.getResult());

  }

  @Test
  public void testNameFieldsQuery2() throws IOException {
    NameUsageWrapperConverter converter = new NameUsageWrapperConverter();

    // Find all documents where the uninomial field is not empty
    NameUsageSearchRequest nsr = new NameUsageSearchRequest();
    nsr.setHighlight(false);
    nsr.addFilter(NameUsageSearchParameter.FIELD, "uninomial", "remarks", "specific_epithet");

    // Match
    Name n = new Name();
    n.setUninomial("laridae");
    BareName bn = new BareName(n);
    NameUsageWrapper nuw1 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw1));

    // Match
    n = new Name();
    n.setUninomial("parus");
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw2 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw2));

    // No match
    n = new Name();
    n.setUninomial(null);
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw3 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw3));

    // Match
    n = new Name();
    n.setUninomial("parus");
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw4 = new NameUsageWrapper(bn);
    insert(client, indexName(), converter.toDocument(nuw4));

    refreshIndex(client, indexName());

    List<NameUsageWrapper> expected = Arrays.asList(nuw1, nuw2, nuw4);

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), nsr, new Page());

    assertEquals(expected, result.getResult());

  }

  @Test
  public void testNameFieldsQuery3() throws IOException {

    // Find all documents where the uninomial field is not empty
    NameUsageSearchRequest query = new NameUsageSearchRequest();
    query.setHighlight(false);
    query.addFilter(NameUsageSearchParameter.FIELD, "uninomial", "remarks", "specific_epithet");

    index(testNameFieldsQuery3_data());

    refreshIndex(client, indexName());

    List<NameUsageWrapper> expected = new ArrayList<>(testNameFieldsQuery3_data());
    expected.remove(3);

    ResultPage<NameUsageWrapper> result = svc.search(indexName(), query, new Page());
    assertEquals(expected, result.getResult());

  }

  private static List<NameUsageWrapper> testNameFieldsQuery3_data() {
    // Match
    Name n = new Name();
    n.setUninomial("laridae");
    BareName bn = new BareName(n);
    NameUsageWrapper nuw1 = new NameUsageWrapper(bn);
    nuw1.setId("11111");

    // Match
    n = new Name();
    n.setUninomial("parus");
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw2 = new NameUsageWrapper(bn);
    nuw2.setId("22222");

    // Match
    n = new Name();
    n.setUninomial(null);
    n.setGenus("parus");
    n.setSpecificEpithet("major");
    n.setRemarks("A bird");
    bn = new BareName(n);
    NameUsageWrapper nuw3 = new NameUsageWrapper(bn);
    nuw3.setId("33333");

    // No Match
    n = new Name();
    n.setUninomial(null);
    n.setGenus("parus");
    bn = new BareName(n);
    NameUsageWrapper nuw4 = new NameUsageWrapper(bn);
    nuw4.setId("44444");

    // Match
    n = new Name();
    n.setUninomial(null);
    n.setGenus("parus");
    n.setRemarks("A bird");
    bn = new BareName(n);
    NameUsageWrapper nuw5 = new NameUsageWrapper(bn);
    nuw5.setId("55555");

    return List.of(nuw1, nuw2, nuw3, nuw4, nuw5);

  }

  @Test
  @Ignore("Fails since https://github.com/Sp2000/colplus-backend/commit/76ac785a29dc39054859a4471e2dbb20bbc9de8b")
  public void testWithBigQ() {
    NameUsageSearchRequest query = new NameUsageSearchRequest();
    query.setHighlight(false);
    query.setQ("ABCDEFGHIJKLMNOPQRSTUVW");
    List<NameUsageWrapper> documents = testWithBigQ_data();
    index(documents);
    NameUsageSearchResponse response = search(query);
    assertEquals(2, response.getResult().size());
  }

  private List<NameUsageWrapper> testWithBigQ_data() {

    List<NameUsageWrapper> usages = createNameUsages(4);

    // Match on scientific name
    usages.get(0).getUsage().getName().setSpecificEpithet("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    // Match on vernacular name
    VernacularName vn = new VernacularName();
    vn.setName("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    usages.get(1).setVernacularNames(Arrays.asList(vn));

    // No match (missing 'W')
    usages.get(2).getUsage().getName().setSpecificEpithet("ABCDEFGHIJKLMNOPQRSTUV");

    // No match (missing 'A')
    usages.get(3).getUsage().getName().setSpecificEpithet("BCDEFGHIJKLMNOPQRSTUVW");

    return usages;

  }

  // Issue #207
  @Test
  public void testWithSmthii__1() {
    NameUsageSearchRequest query = new NameUsageSearchRequest();
    query.setPrefix(true);
    query.setHighlight(false);
    query.setQ("Smithi");
    query.setFuzzy(true);
    index(testWithSmthii_data());
    // Expect all to come back
    NameUsageSearchResponse result = search(query);
    assertEquals(testWithSmthii_data(), result.getResult());
  }

  @Test
  public void testWithSmthii__2() {
    NameUsageSearchRequest query = new NameUsageSearchRequest();
    query.setPrefix(true);
    query.setHighlight(false);
    query.setQ("Smithii");
    query.setFuzzy(true);
    index(testWithSmthii_data());
    // Expect all to come back
    NameUsageSearchResponse result = search(query);
    assertEquals(testWithSmthii_data(), result.getResult());
  }

  private static List<NameUsageWrapper> testWithSmthii_data() {
    Name n = new Name();
    n.setSpecificEpithet("Smithii");
    BareName bn = new BareName(n);
    NameUsageWrapper nuw1 = new NameUsageWrapper(bn);

    n = new Name();
    n.setSpecificEpithet("Smithi");
    bn = new BareName(n);
    NameUsageWrapper nuw2 = new NameUsageWrapper(bn);

    n = new Name();
    n.setSpecificEpithet("SmithiiFooBar");
    bn = new BareName(n);
    NameUsageWrapper nuw3 = new NameUsageWrapper(bn);

    n = new Name();
    n.setSpecificEpithet("SmithiFooBar");
    bn = new BareName(n);
    NameUsageWrapper nuw4 = new NameUsageWrapper(bn);

    return Arrays.asList(nuw1, nuw2, nuw3, nuw4);
  }

}
