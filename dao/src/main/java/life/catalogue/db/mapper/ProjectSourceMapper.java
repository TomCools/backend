package life.catalogue.db.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import life.catalogue.api.model.Dataset;
import life.catalogue.api.model.DOI;
import life.catalogue.api.model.Organisation;
import life.catalogue.api.model.Person;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.api.vocab.DatasetType;
import life.catalogue.api.vocab.License;
import org.apache.ibatis.annotations.Param;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DatasetProcessable refers to archived dataset metadata for projects only!
 * Contrary to its name the table project_source only stores sources for releases.
 * The live project sources are determined based on the sector mappings alone and are the reason for the name of the mapper.
 */
public interface ProjectSourceMapper {

  /**
   * Copies a given dataset into the project archive.
   * The archive requires the source datasets key and project datasetKey combined to be unique.
   *
   * @param datasetKey the project the source dataset belongs to
   * @param d dataset to store as the projects source
   */
  default void create(int datasetKey, Dataset d){
    createInternal(new DatasetWithProjectKey(datasetKey, d));
  }

  void createInternal(DatasetWithProjectKey d);

  /**
   * Retrieves a single released source dataset from the archive
   * @param key the source dataset key
   * @param datasetKey the release dataset key. No project keys allowed!
   */
  Dataset getReleaseSource(@Param("key") int key, @Param("datasetKey") int datasetKey);

  /**
   * Retrieves a single source dataset for a project, reading either from the latest version
   * or the dataset archive if the last successful sync attempt was older.
   * @param key the source dataset key
   * @param datasetKey the project dataset key. No release keys allowed!
   */
  Dataset getProjectSource(@Param("key") int key, @Param("datasetKey") int datasetKey);

  /**
   * @param datasetKey the release dataset key
   */
  List<Dataset> listReleaseSources(@Param("datasetKey") int datasetKey);

  /**
   * Lists all project or release sources retrieving metadata either from the latest version
   * or an archived copy depending on the import attempt of the last sync stored in the sectors.
   */
  List<Dataset> listProjectSources(@Param("datasetKey") int datasetKey);

  /**
   * Deletes all source datasets for the given release
   * @param datasetKey the release dataset key. No project keys allowed!
   */
  int deleteByRelease(@Param("datasetKey") int datasetKey);


  class DatasetWithProjectKey {
    public final int datasetKey;
    public final Dataset dataset;

    public DatasetWithProjectKey(int projectKey, Dataset dataset) {
      this.datasetKey = projectKey;
      this.dataset = dataset;
    }

    public int getDatasetKey() {
      return datasetKey;
    }

    public Integer getKey() {
      return dataset.getKey();
    }

    public DOI getDoi() {
      return dataset.getDoi();
    }

    public DatasetType getType() {
      return dataset.getType();
    }

    public Integer getSourceKey() {
      return dataset.getSourceKey();
    }

    public Integer getImportAttempt() {
      return dataset.getImportAttempt();
    }

    public String getTitle() {
      return dataset.getTitle();
    }

    public String getDescription() {
      return dataset.getDescription();
    }

    public List<Person> getAuthors() {
      return dataset.getCreator();
    }

    public List<Person> getEditors() {
      return dataset.getEditor();
    }

    public List<Organisation> getOrganisations() {
      return dataset.getOrganisations();
    }

    public Person getContact() {
      return dataset.getContact();
    }

    public License getLicense() {
      return dataset.getLicense();
    }

    public String getVersion() {
      return dataset.getVersion();
    }

    public String getGeographicScope() {
      return dataset.getGeographicScope();
    }

    public LocalDate getReleased() {
      return dataset.getIssued();
    }

    public String getCitation() {
      return dataset.getCitation();
    }

    public URI getWebsite() {
      return dataset.getUrl();
    }

    public URI getLogo() {
      return dataset.getLogo();
    }

    public DatasetOrigin getOrigin() {
      return dataset.getOrigin();
    }

    public String getNotes() {
      return dataset.getNotes();
    }

    @JsonIgnore
    public String getAliasOrTitle() {
      return dataset.getAliasOrTitle();
    }

    public String getAlias() {
      return dataset.getAlias();
    }

    public String getGroup() {
      return dataset.getGroup();
    }

    public Integer getConfidence() {
      return dataset.getConfidence();
    }

    public Integer getCompleteness() {
      return dataset.getCompleteness();
    }

    public LocalDateTime getCreated() {
      return dataset.getCreated();
    }

    public Integer getCreatedBy() {
      return dataset.getCreatedBy();
    }

    public LocalDateTime getModified() {
      return dataset.getModified();
    }

    public Integer getModifiedBy() {
      return dataset.getModifiedBy();
    }
  }

}
