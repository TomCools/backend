package life.catalogue.doi.service;

import life.catalogue.api.model.Dataset;
import life.catalogue.api.model.User;
import life.catalogue.doi.datacite.model.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;

/**
 * Converts COL metadata into DataCite metadata.
 * This currently only implements the core basics and waits for the new metadata model to be implemented.
 */
public class DatasetConverter {
  private static final Logger LOG = LoggerFactory.getLogger(DatasetConverter.class);
  private final URI portal;
  private final UriBuilder clbBuilder;
  private final UriBuilder clbSourceBuilder;
  private final UriBuilder portalSourceBuilder;
  private final Function<Integer, User> userByID;

  public DatasetConverter(URI portalURI, URI clbURI, Function<Integer, User> userByID) {
    portal = UriBuilder.fromUri(portalURI).path("data/metadata").build();
    portalSourceBuilder = UriBuilder.fromUri(portalURI).path("data/dataset/{key}");
    clbBuilder = UriBuilder.fromUri(clbURI).path("dataset/{key}");
    clbSourceBuilder = UriBuilder.fromUri(clbURI).path("dataset/{projectKey}/source/{key}");
    this.userByID = userByID;
  }

  /**
   * Populates mandatory attributes:
   *  - title
   *  - publisher
   *  - publicationYear
   *  - creator
   *  - url
   *
   * @param release
   * @param latest
   * @return
   */
  public DoiAttributes release(Dataset release, boolean latest) {
    DoiAttributes attr = new DoiAttributes(release.getDoi());
    // title
    attr.setTitles(List.of(new Title(release.getTitle())));
    // publisher
    attr.setPublisher("Catalogue of Life");
    // PublicationYear
    if (release.getIssued() != null) {
      attr.setPublicationYear(release.getIssued().getYear());
    } else {
      LOG.warn("No release date given. Use today instead");
      attr.setPublicationYear(LocalDate.now().getYear());
    }
    // creator
    if (release.getCreator() != null) {
      attr.setCreators(release.getCreator().stream()
        .map(a -> new Creator(a.getGiven(), a.getFamily(), a.getOrcid()))
        .collect(Collectors.toList())
      );
    } else {
      LOG.warn("No authors given. Use dataset creator instead");
      User user = userByID.apply(release.getCreatedBy());
      Creator creator;
      if (user.getLastname() != null) {
        creator = new Creator(user.getFirstname(), user.getLastname());
      } else {
        creator = new Creator(user.getUsername(), NameType.PERSONAL);
      }
      creator.setNameIdentifier(List.of(NameIdentifier.gbif(user.getUsername())));
      attr.setCreators(List.of(creator));
    }
    // contributors
    if (release.getEditor() != null) {
      attr.setContributors(release.getEditor().stream()
        .map(a -> new Contributor(a.getGiven(), a.getFamily(), a.getOrcid(), ContributorType.EDITOR))
        .collect(Collectors.toList())
      );
    }
    // url
    attr.setUrl(datasetURI(release.getKey(), latest).toString());
    return attr;
  }

  public DoiAttributes source(Dataset source, Dataset project, boolean latest) {
    DoiAttributes attr = release(source, latest);
    attr.setUrl(sourceURI(project.getKey(), source.getKey(), latest).toString());
    return attr;
  }

  public URI datasetURI(int datasetKey, boolean portal) {
    return portal ? this.portal : clbBuilder.build(datasetKey);
  }

  public URI sourceURI(int projectKey, int sourceKey, boolean portal) {
    return portal ? portalSourceBuilder.build(sourceKey) : clbSourceBuilder.build(projectKey, sourceKey);
  }
}
