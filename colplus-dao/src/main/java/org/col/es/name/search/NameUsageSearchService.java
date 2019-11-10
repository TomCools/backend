package org.col.es.name.search;

import java.util.Collections;

import org.col.api.model.Page;
import org.col.api.search.NameUsageSearchRequest;
import org.col.api.search.NameUsageSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NameUsageSearchService {

  Logger LOG = LoggerFactory.getLogger(NameUsageSearchService.class);

  /**
   * Converts the Elasticsearh response coming back from the query into an API object (NameSearchResponse).
   *
   * @param nameSearchRequest
   * @param page
   * @return
   */
  NameUsageSearchResponse search(NameUsageSearchRequest nameSearchRequest, Page page);

  /**
   * @return a pass through search service that never returns any results. Good for tests
   */
  static NameUsageSearchService passThru() {
    return new NameUsageSearchService() {

      @Override
      public NameUsageSearchResponse search(NameUsageSearchRequest request, Page page) {
        LOG.info("No Elastic Search configured. Passing through search request {}", request);
        return new NameUsageSearchResponse(page, 0, Collections.EMPTY_LIST);
      }
    };
  }

}
