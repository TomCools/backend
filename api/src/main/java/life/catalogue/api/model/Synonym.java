package life.catalogue.api.model;

import life.catalogue.api.vocab.TaxonomicStatus;

import java.util.Comparator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;

/**
 * A taxonomic synonym, linking a name to potentially multiple taxa.
 * Can be used for both homo-and heterotypic synonyms as well as misapplied names.
 */
public class Synonym extends NameUsageBase implements Comparable<Synonym> {
  private static final Comparator<Synonym> NATURAL_ORDER = Comparator.<Synonym, Integer>comparing(s -> s.getName().getPublishedInYear(), Comparator.nullsLast(Comparator.naturalOrder()))
                                                                     .thenComparing(s -> s.getName().getLabel());

                                                                     ;
  private Taxon accepted;

  public Synonym() {
  }

  public Synonym(Name n) {
    super(n);
  }

  public Synonym(NameUsageBase other) {
    super(other);
  }

  public Synonym(Synonym other) {
    super(other);
    this.accepted = other.accepted;
  }

  public Synonym(SimpleName sn) {
    super(sn);
  }

  @Override
  public NameUsageBase copy() {
    return new Synonym(this);
  }

  @Override
  public String getLabel(boolean html) {
    return labelBuilder(getName(), accepted != null ? accepted.isExtinct() : null, getStatus(), getNamePhrase(), getAccordingTo(), html).toString();
  }

  @Override
  public void setStatus(TaxonomicStatus status) {
    if (!Preconditions.checkNotNull(status).isSynonym()) {
      throw new IllegalArgumentException("Synonym cannot have a " + status + " status");
    }
    super.setStatus(status);
  }
  
  public Taxon getAccepted() {
    return accepted;
  }
  
  public void setAccepted(Taxon accepted) {
    this.accepted = accepted;
  }

  @Override
  public int compareTo(@NotNull Synonym o) {
    return NATURAL_ORDER.compare(this, o);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    Synonym synonym = (Synonym) o;
    return Objects.equals(accepted, synonym.accepted);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), accepted);
  }
}
