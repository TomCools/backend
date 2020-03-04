package life.catalogue.es.name.search;

import java.util.ArrayList;
import java.util.List;
import life.catalogue.api.search.NameUsageSearchRequest;
import life.catalogue.es.name.QTranslationHelper;
import life.catalogue.es.query.BoolQuery;
import life.catalogue.es.query.Query;
import static life.catalogue.api.search.NameUsageSearchRequest.SearchContent.AUTHORSHIP;
import static life.catalogue.api.search.NameUsageSearchRequest.SearchContent.SCIENTIFIC_NAME;
import static life.catalogue.api.search.NameUsageSearchRequest.SearchContent.VERNACULAR_NAME;

class QTranslator {

  private final NameUsageSearchRequest request;

  QTranslator(NameUsageSearchRequest request) {
    this.request = request;
  }

  Query translate() {
    QTranslationHelper helper = new QTranslationHelper(request);
    List<Query> queries = new ArrayList<Query>(request.getContent().size());
    if (request.getContent().contains(SCIENTIFIC_NAME)) {
      queries.add(helper.getScientificNameQuery());
    }
    if (request.getContent().contains(VERNACULAR_NAME)) {
      queries.add(helper.getVernacularNameQuery());
    }
    if (request.getContent().contains(AUTHORSHIP)) {
      queries.add(helper.getAuthorshipQuery());
    }
    if (queries.size() == 1) {
      return queries.get(0);
    }
    BoolQuery query = new BoolQuery();
    queries.forEach(query::should);
    return query;
  }

}
