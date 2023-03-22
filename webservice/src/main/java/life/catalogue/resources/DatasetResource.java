package life.catalogue.resources;

import life.catalogue.api.exception.NotFoundException;
import life.catalogue.api.model.*;
import life.catalogue.api.search.DatasetSearchRequest;
import life.catalogue.api.vocab.DatasetOrigin;
import life.catalogue.assembly.SyncManager;
import life.catalogue.assembly.SyncState;
import life.catalogue.basgroup.HomotypicConsolidationJob;
import life.catalogue.concurrent.JobExecutor;
import life.catalogue.dao.DatasetDao;
import life.catalogue.dao.DatasetSourceDao;
import life.catalogue.db.mapper.DatasetMapper;
import life.catalogue.dw.auth.Roles;
import life.catalogue.common.ws.MoreMediaTypes;
import life.catalogue.dw.jersey.filter.VaryAccept;
import life.catalogue.release.AuthorlistGenerator;
import life.catalogue.release.ProjectCopyFactory;
import life.catalogue.release.ProjectRelease;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Hidden;

import static life.catalogue.api.model.User.userkey;

@Path("/dataset")
@SuppressWarnings("static-method")
public class DatasetResource extends AbstractGlobalResource<Dataset> {
  private final DatasetDao dao;
  private final DatasetSourceDao sourceDao;
  private final SyncManager assembly;
  private final JobExecutor exec;
  private final ProjectCopyFactory jobFactory;

  private final AuthorlistGenerator authGen = new AuthorlistGenerator();

  public DatasetResource(SqlSessionFactory factory, DatasetDao dao, DatasetSourceDao sourceDao, SyncManager assembly, ProjectCopyFactory jobFactory, JobExecutor exec) {
    super(Dataset.class, dao, factory);
    this.dao = dao;
    this.sourceDao = sourceDao;
    this.assembly = assembly;
    this.jobFactory = jobFactory;
    this.exec = exec;
  }

  /**
   * @return the primary key of the object. Together with the CreatedResponseFilter will return a 201 location
   */
  @POST
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML})
  public Integer createAlt(Dataset obj, @Auth User user) {
    return this.create(obj, user);
  }

  @GET
  @VaryAccept
  public ResultPage<Dataset> search(@Valid @BeanParam Page page, @BeanParam DatasetSearchRequest req, @Auth Optional<User> user) {
    return dao.search(req, userkey(user), page);
  }

  @GET
  @Path("keys")
  public List<Integer> listAllKeys(@BeanParam DatasetSearchRequest req) {
    return dao.searchKeys(req);
  }

  @GET
  @Path("{key}")
  @Override
  @VaryAccept
  @Produces({MediaType.APPLICATION_JSON,
    MediaType.APPLICATION_XML, MediaType.TEXT_XML,
    MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML,
    MoreMediaTypes.APP_JSON_CSL,
    MoreMediaTypes.APP_BIBTEX
  })
  public Dataset get(@PathParam("key") Integer key) {
    return super.get(key);
  }

  @PUT
  @Path("{key}")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML, MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML})
  public void updateAlt(@PathParam("key") Integer key, Dataset obj, @Auth User user) {
    // merge metadata?
    Dataset old;
    try (SqlSession session = factory.openSession(true)){
      DatasetMapper dm = session.getMapper(DatasetMapper.class);
      var settings = dm.getSettings(key);
      old = dm.get(key);
      if (settings == null || old == null) {
        throw NotFoundException.notFound(Dataset.class, key);
      }
      dao.patchMetadata(new DatasetWithSettings(old,settings), obj);
    }
    this.update(key, old, user);
  }

  @GET
  @Path("{key}/{attempt}")
  @VaryAccept
  @Produces({MediaType.APPLICATION_JSON,
    MediaType.APPLICATION_XML, MediaType.TEXT_XML,
    MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML,
    MoreMediaTypes.APP_JSON_CSL,
    MoreMediaTypes.APP_BIBTEX
  })
  public Dataset getMetadataArchive(@PathParam("key") Integer key, @PathParam("attempt") Integer attempt) {
    return dao.getArchive(key, attempt);
  }

  @GET
  @Path("{key}/preview")
  @VaryAccept
  @Produces({MediaType.APPLICATION_JSON,
    MediaType.APPLICATION_XML, MediaType.TEXT_XML,
    MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML,
    MoreMediaTypes.APP_JSON_CSL,
    MoreMediaTypes.APP_BIBTEX
  })
  public Dataset preview(@PathParam("key") Integer key) {
    Dataset d = super.get(key);
    if (d.getOrigin() != DatasetOrigin.PROJECT) {
      throw new IllegalArgumentException("Release metadata preview required a managed project");
    }

    ProjectRelease.modifyDataset(key, d, dao.getSettings(key), sourceDao, authGen);
    return d;
  }

  @GET
  @Path("{key}/settings")
  public DatasetSettings getSettings(@PathParam("key") int key) {
    return dao.getSettings(key);
  }

  @PUT
  @Path("{key}/settings")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void putSettings(@PathParam("key") int key, DatasetSettings settings, @Auth User user) {
    dao.putSettings(key, settings, user.getKey());
  }

  @GET
  @Path("{key}/assembly")
  public SyncState assemblyState(@PathParam("key") int key) {
    return assembly.getState(key);
  }

  @POST
  @Path("/{key}/copy")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void copy(@PathParam("key") int key, @Auth User user) {
    var job = jobFactory.buildDuplication(key, user.getKey());
    exec.submit(job);
  }

  @POST
  @Path("/{key}/consolidate-homotypic")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void homotypicGrouping(@PathParam("key") int key, @QueryParam("taxonID") String taxonID, @Auth User user) {
    HomotypicConsolidationJob job;
    if (StringUtils.isBlank(taxonID)) {
      job = new HomotypicConsolidationJob(factory, key, user.getKey());
    } else {
      job = new HomotypicConsolidationJob(factory, key, user.getKey(), taxonID);
    }
    exec.submit(job);
  }

  @POST
  @Path("/{key}/release")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void release(@PathParam("key") int key, @Auth User user) {
    var job = jobFactory.buildRelease(key, user.getKey());
    exec.submit(job);
  }

  @POST
  @Path("/{key}/xrelease")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  public void xRelease(@PathParam("key") int key, @Auth User user) {
    Integer releaseKey;
    try (SqlSession session = factory.openSession(true)) {
      releaseKey = session.getMapper(DatasetMapper.class).latestRelease(key, true);
    }
    if (releaseKey == null) {
      throw new IllegalArgumentException("Project " + key + " was never released in public");
    }
    var job = jobFactory.buildExtendedRelease(releaseKey, user.getKey());
    exec.submit(job);
  }

  @GET
  @Path("/{key}/source")
  public List<Dataset> projectSources(@PathParam("key") int datasetKey, @QueryParam("notCurrentOnly") boolean notCurrentOnly) {
    var ds = sourceDao.list(datasetKey, null, false);
    if (notCurrentOnly) {
      List<Dataset> notCurrent = new ArrayList<>();
      for (Dataset d : ds) {
        Dataset curr = dao.get(d.getKey());
        if (curr != null && !Objects.equals(curr.getAttempt(), d.getAttempt())) {
          notCurrent.add(d);
        }
      }
      return notCurrent;

    } else {
      return ds;
    }
  }

  @GET
  @Path("/{key}/source/{id}")
  @VaryAccept
  @Produces({MediaType.APPLICATION_JSON,
    MediaType.APPLICATION_XML, MediaType.TEXT_XML,
    MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML,
    MoreMediaTypes.APP_JSON_CSL,
    MoreMediaTypes.APP_BIBTEX
  })
  public Dataset projectSource(@PathParam("key") int datasetKey,
                               @PathParam("id") int id,
                               @QueryParam("original") boolean original) {
    return sourceDao.get(datasetKey, id, original);
  }

  @PUT
  @Path("/{key}/source/{id}")
  @RolesAllowed({Roles.ADMIN, Roles.EDITOR})
  @Consumes({MediaType.APPLICATION_JSON, MoreMediaTypes.APP_YAML, MoreMediaTypes.TEXT_YAML})
  public void updateProjectSource(@PathParam("key") int datasetKey, @PathParam("id") int id, Dataset obj, @Auth User user) {
    if (obj==null) {
      throw new IllegalArgumentException("No source entity given for key " + id);
    }
    obj.setKey(id);
    obj.applyUser(user);
    int i = sourceDao.update(datasetKey, obj, user.getKey());
    if (i == 0) {
      throw NotFoundException.notFound(Dataset.class, DSID.of(datasetKey, id));
    }
  }

  @GET
  @Hidden
  @Path("/{key}/source/{id}/seo")
  @Produces({MediaType.TEXT_PLAIN, MediaType.TEXT_HTML})
  public Response getHtmlHeader(@PathParam("key") int datasetKey, @PathParam("id") int id) {
    var d = sourceDao.get(datasetKey, id, false);
    if (d == null) {
      throw NotFoundException.notFound(Dataset.class, DSID.of(datasetKey, id));
    }
    return ResourceUtils.streamFreemarker(d, "seo/DATASET.ftl", MediaType.TEXT_PLAIN_TYPE);
  }

  @GET
  @Path("/{key}/source/{id}/metrics")
  public ImportMetrics projectSourceMetrics(@PathParam("key") int datasetKey, @PathParam("id") int id) {
    return sourceDao.projectSourceMetrics(datasetKey, id);
  }
}
