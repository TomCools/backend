package org.col.es.name.search;

import java.util.Set;

import org.col.api.search.NameUsageSearchParameter;
import org.col.api.search.NameUsageSearchRequest;
import org.col.es.InvalidQueryException;
import org.col.es.query.BoolQuery;
import org.col.es.query.Query;

/**
 * Translates all query parameters except the "q" parameter into an Elasticsearch query. Unless there is just one query parameter, this will
 * result in an AND query. For example: ?rank=genus&nom_status=available is translated into (rank=genus AND nom_status=available). If a
 * request parameter is multi-valued, this will result in a nested OR query. For example: ?nom_status=available&rank=family&rank=genus is
 * translated into (nom_status=available AND (rank=family OR rank=genus))
 */
class FiltersTranslator {

  private final NameUsageSearchRequest request;

  FiltersTranslator(NameUsageSearchRequest request) {
    this.request = request;
  }

  Query translate() throws InvalidQueryException {
    FilterTranslator ft = new FilterTranslator(request);
    Set<NameUsageSearchParameter> params = request.getFilters().keySet();
    if (params.size() == 1) {
      return params.stream().map(ft::translate).findFirst().get();
    }
    BoolQuery query = new BoolQuery();
    params.stream().map(ft::translate).forEach(query::filter);
    return query;
  }

}
