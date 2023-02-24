package life.catalogue.resources;

import com.univocity.parsers.common.AbstractParser;

import io.dropwizard.auth.Auth;

import life.catalogue.WsServerConfig;
import life.catalogue.api.model.*;
import life.catalogue.api.util.ObjectUtils;
import life.catalogue.api.vocab.Issue;
import life.catalogue.api.vocab.TaxonomicStatus;
import life.catalogue.concurrent.JobExecutor;
import life.catalogue.matching.*;
import life.catalogue.common.ws.MoreMediaTypes;
import life.catalogue.csv.CsvReader;
import life.catalogue.importer.NameInterpreter;

import org.apache.ibatis.session.SqlSessionFactory;

import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/dataset/{key}/nameusage/match")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("static-method")
public class UsageMatchingResource {
  private static final Logger LOG = LoggerFactory.getLogger(UsageMatchingResource.class);

  private final WsServerConfig cfg;
  private final JobExecutor exec;
  private final SqlSessionFactory factory;
  private final UsageMatcherGlobal matcher;
  private final NameInterpreter interpreter = new NameInterpreter(new DatasetSettings());

  public UsageMatchingResource(WsServerConfig cfg, JobExecutor exec, SqlSessionFactory factory, UsageMatcherGlobal matcher) {
    this.cfg = cfg;
    this.exec = exec;
    this.factory = factory;
    this.matcher = matcher;
  }

  private UsageMatchWithOriginal match(int datasetKey, SimpleNameClassified<SimpleName> sn) {
    IssueContainer issues = new IssueContainer.Simple();
    return match(datasetKey, sn, issues);
  }

  private UsageMatchWithOriginal match(int datasetKey, SimpleNameClassified<SimpleName> sn, IssueContainer issues) {
    UsageMatch match;
    var opt = interpreter.interpret(sn, issues);
    if (opt.isPresent()) {
      NameUsageBase nu = (NameUsageBase) NameUsage.create(sn.getStatus(), opt.get().getName());
      match = matcher.match(datasetKey, nu, sn.getClassification());
    } else {
      match = UsageMatch.empty(0);
      issues.addIssue(Issue.UNPARSABLE_NAME);
    }
    return new UsageMatchWithOriginal(match, issues, sn);
  }


  @GET
  public UsageMatchWithOriginal match(@PathParam("key") int datasetKey,
                                      @QueryParam("id") String id,
                                      @QueryParam("q") String q,
                                      @QueryParam("name") String name,
                                      @QueryParam("scientificName") String sciname,
                                      @QueryParam("authorship") String authorship,
                                      @QueryParam("code") NomCode code,
                                      @QueryParam("rank") Rank rank,
                                      @QueryParam("status") @DefaultValue("ACCEPTED") TaxonomicStatus status,
                                      @BeanParam Classification classification
  ) throws InterruptedException {
    if (status == TaxonomicStatus.BARE_NAME) {
      throw new IllegalArgumentException("Cannot match a bare name to a name usage");
    }
    SimpleNameClassified<SimpleName> orig = SimpleNameClassified.snc(id, rank, code, status, ObjectUtils.coalesce(sciname, name, q), authorship);
    if (classification != null) {
      orig.setClassification(classification.asSimpleNames());
    }
    return match(datasetKey, orig);
  }

  private MatchingJob submit(MatchingRequest req, User user) {
    MatchingJob job = new MatchingJob(req, user.getKey(), factory, matcher, cfg);
    exec.submit(job);
    return job;
  }

  private File upload(InputStream data, User user, String format) throws IOException {
    File local = cfg.normalizer.uploadFile(user.getUsername().replaceAll("\\s+", "_"), "." + format);
    Files.copy(data, local.toPath(), StandardCopyOption.REPLACE_EXISTING);
    return local;
  }

  @POST
  @Path("job")
  public MatchingJob matchSourceJob(@PathParam("key") int targetDatasetKey,
                                    @BeanParam MatchingRequest req,
                                    @Auth User user) {
    if (req.getSourceDatasetKey() == null) {
      throw new IllegalArgumentException("sourceDatasetKey parameter or CSV/TSV data upload required");
    }
    return submit(req, user);
  }

  @POST
  @Path("job")
  @Consumes({MoreMediaTypes.TEXT_CSV})
  public MatchingJob matchCsvJob(@PathParam("key") int datasetKey,
                                 InputStream data,
                                 @Auth User user) throws IOException {
    var req = new MatchingRequest();
    req.setDatasetKey(datasetKey);
    req.setUpload(upload(data, user, "csv"));
    return submit(req, user);
  }

  @POST
  @Path("job")
  @Consumes({MediaType.TEXT_PLAIN, MoreMediaTypes.TEXT_TSV})
  public MatchingJob matchTsvJob(@PathParam("key") int datasetKey,
                                      InputStream data,
                                      @Auth User user) throws IOException {
    var req = new MatchingRequest();
    req.setDatasetKey(datasetKey);
    req.setUpload(upload(data, user, "tsv"));
    return submit(req, user);
  }

}
