package life.catalogue.release;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import java.util.*;

public class XReleaseConfig {

  /**
   * XRelease alias with project variables allowed to be applied to a release
   */
  public String alias;

  /**
   * XRelease title with project variables allowed to be applied to a release
   */
  public String title;

  /**
   * XRelease version with project variables allowed to be applied to a release
   */
  public String version;

  /**
   * XRelease description with project variables allowed to be applied to a release
   */
  public String description;

  /**
   * GBIF publisher keys to follow and create sectors for if missing
   */
  public Set<UUID> sourcePublisher;

  /**
   * Selected list of CLB dataset keys to exclude as sectors being created for aboves source publisher keys
   */
  public Set<Integer> sourceDatasetExclusion;

  /**
   * If true algorithmic detecting and grouping of basionyms is executed.
   */
  @Valid
  public boolean groupBasionyms = true;

  /**
   * List of higher wrong homonyms that should be removed, regardless of which source they came from.
   * Map of a canonical name to its direct parent.
   * All other names with the same canonical name, but different parent, are kept.
   *
   * See https://github.com/gbif/checklistbank/issues/93 for more background.
   */
  @NotNull
  @Valid
  public Map<String, List<String>> homonymExclusions = new HashMap<>();

  /**
   * Checks the homonymExclusion list to see if this combination should be excluded.
   * @return true if the name with the given parent should be excluded
   */
  public boolean isExcludedHomonym(String name, String parent) {
    return parent != null && homonymExclusions.getOrDefault(name, Collections.EMPTY_LIST).contains(parent.trim().toUpperCase());
  }

  /**
   * List of epithets, organised by families, which should be ignored during the automated basionym grouping/detection.
   */
  @NotNull
  @Valid
  public Map<String, Set<String>> basionymExclusions = new HashMap<>();


}
