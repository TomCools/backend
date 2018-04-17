package org.col.api.model;

import java.util.Objects;

/**
 *
 */
public class NameAccordingTo {
  private Name name;
  private String accordingTo;

  public Name getName() {
    return name;
  }

  public void setName(Name name) {
    this.name = name;
  }

  public String getAccordingTo() {
    return accordingTo;
  }

  public void setAccordingTo(String accordingTo) {
    this.accordingTo = accordingTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NameAccordingTo that = (NameAccordingTo) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(accordingTo, that.accordingTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, accordingTo);
  }

}
