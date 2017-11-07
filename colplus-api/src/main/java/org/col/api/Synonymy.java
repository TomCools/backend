package org.col.api;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * A taxonomic synonymy list, ordering names in homotypic groups.
 */
public class Synonymy {
  private final List<List<Name>> synonyms;

  public Synonymy() {
    this.synonyms = Lists.newArrayList();
  }

  public List<List<Name>> listHomotypicGroups() {
    return synonyms;
  }

  public void addHomotypicGroup(List<Name> synonyms) {
    this.synonyms.add(synonyms);
  }

  public boolean isEmpty() {
    return synonyms.isEmpty();
  }

  public int size() {
    return synonyms.stream()
        .mapToInt(List::size)
        .sum();
  }
}
