package life.catalogue.api.vocab;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

import java.net.URI;
import java.time.LocalDate;

import com.google.common.base.Preconditions;

import static life.catalogue.api.vocab.DatasetOrigin.*;


/**
 * Dataset settings
 */
public enum Setting {

  /**
   * When importing data from text files this overrides
   * the field delimiter character used
   */
  CSV_DELIMITER(String.class, EXTERNAL),

  /**
   * When importing data from text files this overrides
   * the quote character used
   */
  CSV_QUOTE(String.class, EXTERNAL),

  /**
   * When importing data from text files this overrides
   * the single character used for escaping quotes inside an already quoted value.
   * For example '"' for CSV
   */
  CSV_QUOTE_ESCAPE(String.class, EXTERNAL),

  /**
   * Overrides the gazetteer standard to use in all distribution interpretations for the dataset.
   */
  DISTRIBUTION_GAZETTEER(Gazetteer.class, EXTERNAL, MANAGED),

  /**
   * The nomenclatural code followed in the dataset.
   * It will be used mostly as a hint to format names accordingly.
   * If the dataset contains mixed data from multiple codes keep this field null.
   */
  NOMENCLATURAL_CODE(NomCode.class, EXTERNAL, MANAGED),

  /**
   * Setting that will inform the importer to rematch all decisions (decisions sensu strictu but also sectors and estimates)
   * Defaults to false
   */
  REMATCH_DECISIONS(Boolean.class, EXTERNAL, MANAGED),

  /**
   * Setting that will inform the importer not to update any metadata from archives.
   * Metadata will be locked and can only be edited manually.
   */
  LOCK_METADATA(Boolean.class, EXTERNAL, MANAGED),

  /**
   * Template used to build a new release alias.
   * See RELEASE_TITLE_TEMPLATE for usage.
   */
  RELEASE_ALIAS_TEMPLATE(String.class, MANAGED),

  /**
   * If true a release will include as its authors all authors of all it's sources.
   */
  RELEASE_ADD_SOURCE_AUTHORS(Boolean.class, MANAGED),

  /**
   * If true a release will include as its authors all contributors of the project (not source contributors).
   */
  RELEASE_ADD_CONTRIBUTORS(Boolean.class, MANAGED),

  /**
   * If true a release will first delete all bare names from the project before it copies data.
   */
  RELEASE_REMOVE_BARE_NAMES(Boolean.class, MANAGED),

  /**
   * Number of first authors from a project/release to use for the container authors of a source chapter-in-a-book citation.
   * If not given all authors are used.
   */
  SOURCE_MAX_CONTAINER_AUTHORS(Integer.class, MANAGED, RELEASED),

  DATA_FORMAT(DataFormat.class, EXTERNAL, MANAGED),

  /**
   * In continuous import mode the frequency the dataset is scheduled for imports.
   */
  IMPORT_FREQUENCY(Frequency.class, EXTERNAL),

  DATA_ACCESS(URI.class, EXTERNAL),

  /**
   * Project defaults to be used for the sector.entities property
   */
  SECTOR_ENTITIES(EntityType.class, true, MANAGED),

  /**
   * Project defaults to be used for the sector.ranks property
   */
  SECTOR_RANKS(Rank.class, true, MANAGED),

  /**
   * If set to true the dataset metadata is locked and the gbif registry sync will not be applied to the dataset.
   */
  GBIF_SYNC_LOCK(Boolean.class, false, EXTERNAL);

  private final Class type;
  private final DatasetOrigin[] origin;
  private final boolean multiple;

  public Class getType() {
    return type;
  }

  public DatasetOrigin[] getOrigin() {
    return origin;
  }

  public boolean isEnum() {
    return type.isEnum();
  }

  public boolean isMultiple() {
    return multiple;
  }

  Setting(Class type, DatasetOrigin... origin) {
    this(type, false, origin);
  }
  /**
   * Use String, Integer, Boolean, LocalDate, URI or a custom col enumeration class
   *
   * @param type
   * @param origin
   */
  Setting(Class type, boolean multiple, DatasetOrigin... origin) {
    this.multiple = multiple;
    this.origin = origin;
    Preconditions.checkArgument(type.equals(String.class)
      || type.equals(Integer.class)
      || type.equals(Boolean.class)
      || type.equals(LocalDate.class)
      || type.equals(URI.class)
      || type.isEnum(), "Unsupported type");
    this.type = type;
  }

}
