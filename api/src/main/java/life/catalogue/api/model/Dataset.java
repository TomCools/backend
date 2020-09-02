package life.catalogue.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadata about a dataset or a subset of it if parentKey is given.
 */
public class Dataset extends ArchivedDataset {

  private boolean privat = false;
  private UUID gbifKey;
  private UUID gbifPublisherKey;
  private LocalDateTime imported; // from import table
  private LocalDateTime deleted;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Integer size;

  public Dataset() {
  }

  public Dataset(ArchivedDataset other) {
    super(other);
  }

  public Dataset(Dataset other) {
    super(other);
    this.privat = other.privat;
    this.gbifKey = other.gbifKey;
    this.gbifPublisherKey = other.gbifPublisherKey;
    this.imported = other.imported;
    this.deleted = other.deleted;
    this.size = other.size;
  }

  public UUID getGbifKey() {
    return gbifKey;
  }
  
  public void setGbifKey(UUID gbifKey) {
    this.gbifKey = gbifKey;
  }
  
  public UUID getGbifPublisherKey() {
    return gbifPublisherKey;
  }
  
  public void setGbifPublisherKey(UUID gbifPublisherKey) {
    this.gbifPublisherKey = gbifPublisherKey;
  }
  
  @JsonProperty("private")
  public boolean isPrivat() {
    return privat;
  }

  public void setPrivat(boolean privat) {
    this.privat = privat;
  }

  public Integer getSize() {
    return size;
  }

  /**
   * Time the data of the dataset was last changed in the Clearinghouse,
   * i.e. time of the last import that changed at least one record.
   */
  public LocalDateTime getImported() {
    return imported;
  }

  public void setImported(LocalDateTime imported) {
    this.imported = imported;
  }


  public LocalDateTime getDeleted() {
    return deleted;
  }

  @JsonIgnore
  public boolean hasDeletedDate() {
    return deleted != null;
  }
  
  public void setDeleted(LocalDateTime deleted) {
    this.deleted = deleted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Dataset)) return false;
    if (!super.equals(o)) return false;
    Dataset dataset = (Dataset) o;
    return privat == dataset.privat &&
      Objects.equals(gbifKey, dataset.gbifKey) &&
      Objects.equals(gbifPublisherKey, dataset.gbifPublisherKey) &&
      Objects.equals(imported, dataset.imported) &&
      Objects.equals(deleted, dataset.deleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), privat, gbifKey, gbifPublisherKey, imported, deleted);
  }

  @Override
  public String toString() {
    return "Dataset " + getKey() + ": " + getTitle();
  }
}
