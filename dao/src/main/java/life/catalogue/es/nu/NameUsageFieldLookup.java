package life.catalogue.es.nu;

import life.catalogue.api.search.NameUsageSearchParameter;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static life.catalogue.api.search.NameUsageSearchParameter.*;

/**
 * Maps a name search parameter the corresponding Elasticsearch field(s). In principle a name search parameter may be mapped to multiple
 * Elasticsearch fields, in which case the parameter's value is searched in all of these fields. In practice, though, we currently don't
 * have multiply-mapped name search parameters.
 */
public class NameUsageFieldLookup extends EnumMap<NameUsageSearchParameter, String[]> {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageFieldLookup.class);

  public static final NameUsageFieldLookup INSTANCE = new NameUsageFieldLookup();

  private NameUsageFieldLookup() {
    super(NameUsageSearchParameter.class);
    putSingle(ALPHAINDEX, "nameStrings.sciNameLetter");
    putSingle(AUTHORSHIP, "authorship");
    putSingle(AUTHORSHIP_YEAR, "authorshipYear");
    putSingle(CATALOGUE_KEY, "decisions.catalogueKey");
    putSingle(DATASET_KEY, "datasetKey");
    putSingle(DECISION_MODE, "decisions.mode");
    putSingle(EXTINCT, "extinct");
    putSingle(FIELD, "nameFields");
    putSingle(ISSUE, "issues");
    putSingle(ENVIRONMENT, "environments");
    putSingle(NAME_ID, "nameId");
    putSingle(NOM_CODE, "nomCode");
    putSingle(NOM_STATUS, "nomStatus");
    putSingle(ORIGIN, "origin");
    putSingle(PUBLISHED_IN_ID, "publishedInId");
    putSingle(PUBLISHER_KEY, "publisherKey");
    putSingle(RANK, "rank");
    putSingle(STATUS, "status");
    putSingle(SECTOR_DATASET_KEY, "sectorDatasetKey");
    putSingle(SECTOR_KEY, "sectorKey");
    putSingle(TAXON_ID, "classificationIds");
    putSingle(NAME_TYPE, "type");
    putSingle(USAGE_ID, "usageId");

    // UNSAFE is not mapped, it will never make it through
    if (size()+1 != NameUsageSearchParameter.values().length) {
      Set<NameUsageSearchParameter> all = new HashSet<>(List.of(NameUsageSearchParameter.values()));
      all.removeAll(keySet());
      String missing = all.stream().map(Enum::toString).collect(Collectors.joining(","));
      String msg = "Not all name search parameters mapped to document fields: " + missing;
      LOG.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  public String lookupSingle(NameUsageSearchParameter param) {
    // every NameSearchParameter maps to just one field in the name usage document
    return get(param)[0];
  }

  public String[] lookup(NameUsageSearchParameter param) {
    return get(param);
  }

  private void putSingle(NameUsageSearchParameter param, String field) {
    put(param, new String[] {field});
  }

}
