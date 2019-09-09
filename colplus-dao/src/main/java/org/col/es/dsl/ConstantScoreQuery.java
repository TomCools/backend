package org.col.es.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConstantScoreQuery implements Query {

  private static class Filter {
    @JsonProperty("filter")
    private final Query query;

    Filter(Query query) {
      this.query = query;
    }
  }

  @JsonProperty("constant_score")
  private final Filter filter;

  public ConstantScoreQuery(Query query) {
    this.filter = new Filter(query);
  }

}
