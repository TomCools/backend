package life.catalogue.matching.authorship;

import it.unimi.dsi.fastutil.Pair;

import life.catalogue.api.model.FormattableName;

import life.catalogue.api.model.ScientificName;

import org.gbif.nameparser.api.Authorship;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;

/**
 * A utility to sort a queue of parsed names into sets sharing the same basionym judging only the authorship not epithets.
 * A name without any authorship at all will be ignored and not returned in any group.
 */
public class BasionymSorter {
  private static final Logger LOG = LoggerFactory.getLogger(BasionymSorter.class);
  private AuthorComparator authorComp;
  
  public BasionymSorter(AuthorComparator authorComp) {
    this.authorComp = authorComp;
  }
  
  public static class MultipleBasionymException extends Exception {
  
  }
  
  public Collection<BasionymGroup<FormattableName>> groupBasionyms(Iterable<FormattableName> names) {
    return groupBasionyms(names, Functions.identity(), b->{});
  }
  
  private <T> BasionymGroup<T> findExistingGroup(T p, List<BasionymGroup<T>> groups, Function<T, FormattableName> func) {
    FormattableName pn = func.apply(p);
    for (BasionymGroup<T> g : groups) {
      FormattableName representative = func.apply(g.getRecombinations().get(0));
      if (authorComp.compareStrict(pn.getBasionymAuthorship(), representative.getBasionymAuthorship())) {
        return g;
      }
    }
    return null;
  }

  /**
   * Tries to set the basionym and basionym duplicates, i.e. same names with the same rank, canonical name & authorship (small variation allowed).
   * @throws MultipleBasionymException when multiple combinations are found that all seem to be the basionym
   */
  private <T> void determineBasionym(BasionymGroup<T> group, List<T> originals, Function<T, FormattableName> func) throws MultipleBasionymException {
    var authorship = group.getAuthorship();
    List<T> basionyms = new ArrayList<>();
    for (T obj : originals) {
      FormattableName b = func.apply(obj);
      if (authorComp.compareStrict(authorship, b.getCombinationAuthorship())) {
        basionyms.add(obj);
      }
    }
    if (basionyms.isEmpty()) {
      // try again without year in case we didn't find any but make sure we only match once!
      if (authorship != null) {
        Authorship aNoYear = copyWithoutYear(authorship);
        for (T obj : originals) {
          FormattableName b = func.apply(obj);
          if (authorComp.compareStrict(aNoYear, copyWithoutYear(b.getCombinationAuthorship()))) {
            basionyms.add(obj);
          }
        }
      }
    }
    
    if (basionyms.isEmpty()) {
      group.setBasionym(null);
    } else if (basionyms.size() == 1) {
      group.setBasionym(basionyms.get(0));
    } else {
      // check if we have only true duplicates, i.e. the same combination & rank
      boolean duplicates = true;
      var iter = basionyms.iterator();
      var b1 = func.apply(iter.next());
      while (iter.hasNext()) {
        var b = func.apply(iter.next());
        if (b1.getRank() != b.getRank()
            || !Objects.equals(b1.getGenus(), b.getGenus())
            || (b1.isTrinomial() && !Objects.equals(b1.getSpecificEpithet(), b.getSpecificEpithet()))
        ) {
          duplicates = false;
          break;
        }
      }
      if (duplicates) {
        // randomly pick first as basionym
        group.setBasionym(basionyms.remove(0));
        group.getBasionymDuplicates().addAll(basionyms);
      } else {
        // we have more than one match, dont use it!
        throw new MultipleBasionymException();
      }
    }
  }
  
  private static Authorship copyWithoutYear(Authorship a) {
    Authorship a2 = new Authorship();
    a2.setAuthors(a.getAuthors());
    a2.setExAuthors(a.getExAuthors());
    return a2;
  }
  
  /**
   * Grouping that allows to use any custom class as long as there is a function that returns a Name instance.
   * The queue of groups returned only contains groups with no or one known basionym. Any uncertain cases like groups with multiple basionyms are excluded!
   * @param multiBasionyConsumer consumer that handles the otherwise ignored names (first=originals, second=recombinations) that have multiple basionyms
   */
  public <T> Collection<BasionymGroup<T>> groupBasionyms(Iterable<T> names, Function<T, FormattableName> func, Consumer<Pair<List<T>, List<T>>> multiBasionyConsumer) {
    List<BasionymGroup<T>> groups = new ArrayList<>();
    // first split names into recombinations and original names not having a basionym authorship
    // note that we drop any name without authorship here!
    List<T> recombinations = new ArrayList<>();
    List<T> originals = new ArrayList<>();
    for (T obj : names) {
      ScientificName p = func.apply(obj);
      if (p != null) {
        if (!p.getBasionymAuthorship().isEmpty()) {
          recombinations.add(obj);
        } else if (!p.getCombinationAuthorship().isEmpty()) {
          originals.add(obj);
        }
      } else {
        LOG.warn("No parsed name returned for name object {}", obj);
      }
    }
    
    // now group the recombinations
    for (T recomb : recombinations) {
      BasionymGroup<T> group = findExistingGroup(recomb, groups, func);
      // create new group if needed
      if (group == null) {
        FormattableName pn = func.apply(recomb);
        if (pn != null) {
          group = new BasionymGroup<T>(pn.getTerminalEpithet(), pn.getBasionymAuthorship());
          groups.add(group);
          group.getRecombinations().add(recomb);
        } else {
          LOG.warn("No parsed name returned for name recombination {}", recomb);
        }
      } else {
        group.getRecombinations().add(recomb);
      }
    }
    // finally try to find the basionym for each group in the queue of original names
    Iterator<BasionymGroup<T>> iter = groups.iterator();
    while (iter.hasNext()) {
      BasionymGroup<T> group = iter.next();
      try {
        determineBasionym(group, originals, func);
      } catch (MultipleBasionymException e) {
        LOG.info("Ignore group with multiple basionyms found for {} {} in {} original names", group.getEpithet(), group.getAuthorship(), originals.size());
        iter.remove();
        multiBasionyConsumer.accept(Pair.of(originals, recombinations));
      }
    }
    return groups;
  }
}
