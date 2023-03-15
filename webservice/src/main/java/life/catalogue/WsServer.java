package life.catalogue;

import life.catalogue.api.jackson.ApiModule;
import life.catalogue.api.model.JobResult;
import life.catalogue.api.util.ObjectUtils;
import life.catalogue.assembly.SyncFactory;
import life.catalogue.assembly.SyncManager;
import life.catalogue.matching.UsageMatcherGlobal;
import life.catalogue.cache.CacheFlush;
import life.catalogue.cache.UsageCache;
import life.catalogue.coldp.ColdpTerm;
import life.catalogue.command.*;
import life.catalogue.common.io.DownloadUtil;
import life.catalogue.common.tax.AuthorshipNormalizer;
import life.catalogue.concurrent.ExecutorUtils;
import life.catalogue.concurrent.JobExecutor;
import life.catalogue.concurrent.NamedThreadFactory;
import life.catalogue.dao.*;
import life.catalogue.db.LookupTables;
import life.catalogue.db.tree.DatasetDiffService;
import life.catalogue.db.tree.SectorDiffService;
import life.catalogue.doi.DoiUpdater;
import life.catalogue.doi.service.DataCiteService;
import life.catalogue.doi.service.DatasetConverter;
import life.catalogue.doi.service.DoiService;
import life.catalogue.dw.auth.AuthBundle;
import life.catalogue.dw.cors.CorsBundle;
import life.catalogue.dw.db.MybatisBundle;
import life.catalogue.dw.health.*;
import life.catalogue.dw.jersey.ColJerseyBundle;
import life.catalogue.dw.mail.MailBundle;
import life.catalogue.dw.managed.Component;
import life.catalogue.dw.managed.ManagedService;
import life.catalogue.dw.managed.ManagedUtils;
import life.catalogue.dw.metrics.GangliaBundle;
import life.catalogue.dw.metrics.HttpClientBuilder;
import life.catalogue.es.EsClientFactory;
import life.catalogue.es.NameUsageIndexService;
import life.catalogue.es.NameUsageSearchService;
import life.catalogue.es.NameUsageSuggestionService;
import life.catalogue.es.nu.NameUsageIndexServiceEs;
import life.catalogue.es.nu.search.NameUsageSearchServiceEs;
import life.catalogue.es.nu.suggest.NameUsageSuggestionServiceEs;
import life.catalogue.exporter.ExportManager;
import life.catalogue.gbifsync.GbifSyncManager;
import life.catalogue.img.ImageService;
import life.catalogue.img.ImageServiceFS;
import life.catalogue.importer.ContinuousImporter;
import life.catalogue.importer.ImportManager;
import life.catalogue.matching.NameIndex;
import life.catalogue.matching.NameIndexFactory;
import life.catalogue.metadata.DoiResolver;
import life.catalogue.parser.NameParser;
import life.catalogue.portal.PortalPageRenderer;
import life.catalogue.release.ProjectCopyFactory;
import life.catalogue.release.PublicReleaseListener;
import life.catalogue.resources.*;
import life.catalogue.resources.legacy.IdMap;
import life.catalogue.resources.legacy.LegacyWebserviceResource;
import life.catalogue.resources.parser.*;
import life.catalogue.swagger.OpenApiFactory;

import org.gbif.dwc.terms.TermFactory;

import java.io.IOException;
import java.sql.Connection;

import javax.validation.Validator;
import javax.ws.rs.client.Client;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.ibatis.session.SqlSessionFactory;
import org.elasticsearch.client.RestClient;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.client.spi.ConnectorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import io.dropwizard.Application;
import io.dropwizard.client.DropwizardApacheConnector;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class WsServer extends Application<WsServerConfig> {
  private static final Logger LOG = LoggerFactory.getLogger(WsServer.class);

  private final ColJerseyBundle coljersey = new ColJerseyBundle();
  private final MybatisBundle mybatis = new MybatisBundle();
  private final MailBundle mail = new MailBundle();
  private final AuthBundle auth = new AuthBundle();
  private final EventBus bus = new AsyncEventBus("server-bus",
    ExecutorUtils.newCachedThreadPool(3, 25, new NamedThreadFactory("bus"))
  );
  protected CloseableHttpClient httpClient;
  protected Client jerseyClient;
  private NameIndex ni;
  private ImportManager importManager;

  public static void main(final String[] args) throws Exception {
    SLF4JBridgeHandler.install();
    new WsServer().run(args);
  }

  @Override
  public void initialize(Bootstrap<WsServerConfig> bootstrap) {
    // our mybatis classes
    bootstrap.addBundle(mybatis);
    // various custom jersey providers
    bootstrap.addBundle(coljersey);
    bootstrap.addBundle(mail);
    bootstrap.addBundle(new MultiPartBundle());
    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(new GangliaBundle());

    // authentication which requires the UserMapper from mybatis AFTER the mybatis bundle has run
    bootstrap.addBundle(auth);
    // register CoLTerms
    TermFactory.instance().registerTermEnum(ColdpTerm.class);
    // use a custom jackson mapper
    ObjectMapper om = ApiModule.configureMapper(Jackson.newMinimalObjectMapper());
    bootstrap.setObjectMapper(om);

    // add some cli commands not accessible via the admin interface
    bootstrap.addCommand(new AddTableCmd());
    bootstrap.addCommand(new ExecSqlCmd());
    bootstrap.addCommand(new IndexCmd());
    bootstrap.addCommand(new InitCmd());
    bootstrap.addCommand(new NamesIndexCmd());
    bootstrap.addCommand(new UpdMetricCmd());
    bootstrap.addCommand(new ExportCmd());
    bootstrap.addCommand(new ExportSourcesCmd());
    bootstrap.addCommand(new DoiUpdateCmd());
    bootstrap.addCommand(new RepartitionCmd());
    bootstrap.addCommand(new ArchiveCmd());
    bootstrap.addCommand(new UpdDatasetKeyCheckCmd());
  }

  @Override
  public String getName() {
    return "COLServer";
  }

  public String getUserAgent(WsServerConfig cfg) {
    return getName() + "/" + ObjectUtils.coalesce(cfg.versionString(), "1.0");
  }

  /**
   * Make sure to call this after the app has been bootstrapped, otherwise its null. Methods to add tasks or healthchecks
   * can be sure to use the session factory.
   */
  public SqlSessionFactory getSqlSessionFactory() {
    return mybatis.getSqlSessionFactory();
  }

  @VisibleForTesting
  public AuthBundle getAuthBundle() {
    return auth;
  }

  public EventBus getBus() {
    return bus;
  }

  public NameIndex getNamesIndex() {
    return ni;
  }

  public ImportManager getImportManager() {
    return importManager;
  }

  @Override
  public void run(WsServerConfig cfg, Environment env) throws Exception {
    final JerseyEnvironment j = env.jersey();

    cfg.logDirectories();
    if (cfg.mkdirs()) {
      LOG.info("Created config repository directories");
    }
    clearTmp(cfg);

    // update model configs
    JobResult.setDownloadConfigs(cfg.job.downloadURI, cfg.job.downloadDir);

    // create a managed service that controls our startable/stoppable components in sync with the DW lifecycle
    final ManagedService managedService = new ManagedService(env.lifecycle());

    // update name parser timeout settings
    NameParser.PARSER.setTimeout(cfg.parserTimeout);

    // http client pool is managed via DW lifecycle already
    // use a custom metrics naming strategy that does not involve the user agent name with a version
    httpClient = new HttpClientBuilder(env)
      .using(cfg.client)
      .build(getUserAgent(cfg));

    // reuse the same http client pool also for jersey clients!
    JerseyClientBuilder builder = new JerseyClientBuilder(env)
        .withProperty(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, Boolean.TRUE)
        .withProperty(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.BUFFERED)
        .using(cfg.client)
        .using((ConnectorProvider) (cl,
            runtimeConfig) -> new DropwizardApacheConnector(httpClient, requestConfig(cfg.client), cfg.client.isChunkedEncodingEnabled()));
    // build both syncroneous and reactive clients sharing the same thread pool
  
    jerseyClient = builder.build(getUserAgent(cfg));

    // finally provide the SqlSessionFactory & http client to the auth and jersey bundle
    coljersey.setSqlSessionFactory(mybatis.getSqlSessionFactory());
    auth.setSqlSessionFactory(mybatis.getSqlSessionFactory());
    auth.setClient(httpClient);

    DatasetInfoCache.CACHE.setFactory(mybatis.getSqlSessionFactory());

    // validation
    Validator validator = env.getValidator();

    // job executor
    UserDao udao = new UserDao(getSqlSessionFactory(), bus, validator);
    JobExecutor executor = new JobExecutor(cfg.job, env.metrics(), mail.getEmailNotification(), udao);
    managedService.manage(Component.JobExecutor, executor);

    // name parser
    ParserConfigDao dao = new ParserConfigDao(getSqlSessionFactory());
    dao.loadParserConfigs();
    NameParser.PARSER.register(env.metrics());
    env.healthChecks().register("name-parser", new NameParserHealthCheck());
    env.lifecycle().manage(ManagedUtils.from(NameParser.PARSER));

    // CSL Util
    env.healthChecks().register("csl-utils", new CslUtilsHealthCheck());

    // ES
    NameUsageIndexService indexService;
    NameUsageSearchService searchService;
    NameUsageSuggestionService suggestService;
    if (cfg.es == null) {
      LOG.warn("No Elastic Search configured, use pass through indexing & searching");
      indexService = NameUsageIndexService.passThru();
      searchService = NameUsageSearchService.passThru();
      suggestService = NameUsageSuggestionService.passThru();
    } else {
      final RestClient esClient = new EsClientFactory(cfg.es).createClient();
      env.lifecycle().manage(ManagedUtils.from(esClient));
      env.healthChecks().register("elastic", new EsHealthCheck(esClient, cfg.es));
      indexService = new NameUsageIndexServiceEs(esClient, cfg.es, cfg.normalizer.scratchDir("nuproc"), getSqlSessionFactory());
      searchService = new NameUsageSearchServiceEs(cfg.es.nameUsage.name, esClient);
      suggestService = new NameUsageSuggestionServiceEs(cfg.es.nameUsage.name, esClient);
    }

    // images
    final ImageService imgService = new ImageServiceFS(cfg.img);

    // name index
    ni = NameIndexFactory.persistentOrMemory(cfg.namesIndexFile, getSqlSessionFactory(), AuthorshipNormalizer.INSTANCE);
    // we do not start up the index automatically, we need to run 2 apps in parallel during deploys!
    managedService.manage(Component.NamesIndex, ni);
    env.healthChecks().register("names-index", new NamesIndexHealthCheck(ni));

    final DatasetImportDao diDao = new DatasetImportDao(getSqlSessionFactory(), cfg.metricsRepo);
    final SectorImportDao siDao = new SectorImportDao(getSqlSessionFactory(), cfg.metricsRepo);
    final FileMetricsDatasetDao fmdDao = new FileMetricsDatasetDao(getSqlSessionFactory(), cfg.metricsRepo);
    final FileMetricsSectorDao fmsDao = new FileMetricsSectorDao(getSqlSessionFactory(), cfg.metricsRepo);

    // diff
    DatasetDiffService dDiff = new DatasetDiffService(getSqlSessionFactory(), fmdDao, cfg.diffTimeout);
    SectorDiffService sDiff = new SectorDiffService(getSqlSessionFactory(), fmsDao, cfg.diffTimeout);
    env.healthChecks().register("dataset-diff", new DiffHealthCheck(dDiff));
    env.healthChecks().register("sector-diff", new DiffHealthCheck(sDiff));

    // update db lookups
    try (Connection c = mybatis.getConnection()) {
      LookupTables.recreateTables(c);
    }

    // DOI resolver
    DoiResolver doiResolver = new DoiResolver(httpClient);

    // daos
    AuthorizationDao adao = new AuthorizationDao(getSqlSessionFactory(), bus);
    DatasetExportDao exdao = new DatasetExportDao(cfg.job, getSqlSessionFactory(), bus, validator);
    DatasetDao ddao = new DatasetDao(cfg.db.minExternalDatasetKey, getSqlSessionFactory(), cfg.normalizer, cfg.release, new DownloadUtil(httpClient), imgService, diDao, exdao, indexService, cfg.normalizer::scratchFile, bus, validator);
    DatasetSourceDao dsdao = new DatasetSourceDao(getSqlSessionFactory());
    DecisionDao decdao = new DecisionDao(getSqlSessionFactory(), indexService, validator);
    DuplicateDao dupeDao = new DuplicateDao(getSqlSessionFactory());
    EstimateDao edao = new EstimateDao(getSqlSessionFactory(), validator);
    NameDao ndao = new NameDao(getSqlSessionFactory(), indexService, ni, validator);
    ReferenceDao rdao = new ReferenceDao(getSqlSessionFactory(), doiResolver, validator);
    TaxonDao tdao = new TaxonDao(getSqlSessionFactory(), ndao, indexService, validator);
    SectorDao secdao = new SectorDao(getSqlSessionFactory(), indexService, tdao, validator);
    tdao.setSectorDao(secdao);
    SynonymDao sdao = new SynonymDao(getSqlSessionFactory(), validator);
    TreeDao trDao = new TreeDao(getSqlSessionFactory(), searchService);

    // usage cache
    UsageCache uCache = UsageCache.mapDB(cfg.usageCacheFile, true, false, 64);
    managedService.manage(Component.UsageCache, uCache);

    // matcher
    final var matcher = new UsageMatcherGlobal(ni, uCache, getSqlSessionFactory());

    // DOI
    DoiService doiService;
    if (cfg.doi == null) {
      doiService = DoiService.passThru();
      LOG.warn("DataCite DOI service not configured!");
    } else {
      doiService = new DataCiteService(cfg.doi, jerseyClient, mail.getMailer(), cfg.job.onErrorTo, cfg.job.onErrorFrom);
    }
    DatasetConverter converter = new DatasetConverter(cfg.portalURI, cfg.clbURI, udao::get);
    DoiUpdater doiUpdater = new DoiUpdater(getSqlSessionFactory(), doiService, coljersey.getCache(), converter);

    // portal html page renderer
    PortalPageRenderer renderer = new PortalPageRenderer(ddao, dsdao, tdao, coljersey.getCache(), cfg.portalTemplateDir.toPath());

    // exporter
    ExportManager exportManager = new ExportManager(cfg, getSqlSessionFactory(), executor, imgService, exdao, diDao);

    // syncs and releases
    final var syncFactory = new SyncFactory(getSqlSessionFactory(), ni, matcher, secdao, siDao, edao, indexService);
    final var copyFactory = new ProjectCopyFactory(httpClient, ni, syncFactory, diDao, ddao, siDao, ndao, secdao, exportManager, indexService, imgService, doiService, doiUpdater, getSqlSessionFactory(), validator, cfg);

    // importer
    importManager = new ImportManager(cfg,
      env.metrics(),
      httpClient,
      getSqlSessionFactory(),
      ni,
      ddao, secdao, decdao,
      uCache,
      indexService,
      imgService,
      executor,
      validator, doiResolver
    );
    managedService.manage(Component.DatasetImporter, importManager);

    // continuous importer
    ContinuousImporter cImporter = new ContinuousImporter(cfg.importer, importManager, getSqlSessionFactory());
    managedService.manage(Component.ImportScheduler, cImporter);

    // gbif sync
    GbifSyncManager gbifSync = new GbifSyncManager(cfg.gbif, ddao, getSqlSessionFactory(), jerseyClient);
    managedService.manage(Component.GBIFRegistrySync, gbifSync);

    // assembly
    SyncManager assembly = new SyncManager(getSqlSessionFactory(), ni, syncFactory, env.metrics());
    managedService.manage(Component.SectorSynchronizer, assembly);

    // link assembly and import manager so they are aware of each other
    importManager.setAssemblyCoordinator(assembly);
    assembly.setImportManager(importManager);

    // legacy ID map
    IdMap idMap = IdMap.fromURI(cfg.legacyIdMapFile, cfg.legacyIdMapURI);
    managedService.manage(Component.LegacyIdMap, idMap);

    // resources
    j.register(new AdminResource(
      getSqlSessionFactory(), managedService, assembly, new DownloadUtil(httpClient), cfg, imgService, ni, indexService,
      importManager, ddao, gbifSync, executor, idMap, validator)
    );
    j.register(new DataPackageResource());
    j.register(new DatasetArchiveResource(cfg));
    j.register(new DatasetDiffResource(dDiff));
    j.register(new DatasetEditorResource(adao));
    j.register(new DatasetExportResource(getSqlSessionFactory(), searchService, exportManager, diDao, cfg));
    j.register(new DatasetImportResource(diDao));
    j.register(new DatasetPatchResource());
    j.register(new DatasetResource(getSqlSessionFactory(), ddao, dsdao, assembly, copyFactory, executor));
    j.register(new DatasetReviewerResource(adao));
    j.register(new DecisionResource(decdao));
    j.register(new DocsResource(cfg, OpenApiFactory.build(cfg, env)));
    j.register(new DuplicateResource(dupeDao));
    j.register(new EstimateResource(edao));
    j.register(new ExportResource(exdao, cfg));
    j.register(new ImageResource(imgService));
    j.register(new ImageResourceLegacy(imgService));
    j.register(new ImporterResource(cfg, importManager, diDao, ddao));
    j.register(new JobResource(cfg.job, executor));
    j.register(new LegacyWebserviceResource(cfg, idMap, env.metrics(), getSqlSessionFactory()));
    j.register(new NameMatchingResource());
    j.register(new NamesIndexResource(ni, getSqlSessionFactory(), cfg, executor));
    j.register(new NameResource(ndao));
    j.register(new NameUsageResource(searchService, suggestService));
    j.register(new NameUsageSearchResource(searchService));
    j.register(new PortalResource(renderer));
    j.register(new ReferenceResource(rdao));
    j.register(new SectorDiffResource(sDiff));
    j.register(new SectorResource(secdao, tdao, fmsDao, assembly));
    j.register(new SynonymResource(sdao));
    j.register(new TaxonResource(tdao));
    j.register(new TreeResource(tdao, trDao));
    j.register(new UserResource(auth.getJwtCodec(), udao));
    j.register(new UsageMatchingResource(cfg, executor, getSqlSessionFactory(), matcher));
    j.register(new VerbatimResource());
    j.register(new VernacularGlobalResource());
    j.register(new VernacularResource());
    j.register(new VocabResource());

    // parsers
    j.register(new HomotypicGroupingResource());
    j.register(new HomoglyphParserResource());
    j.register(new NameParserResource(getSqlSessionFactory()));
    j.register(new MetadataParserResource());
    j.register(new ParserResource<>());
    j.register(new ReferenceParserResource(doiResolver));

    j.register(new IdEncoderResource());

    // attach listeners to event bus
    bus.register(auth);
    bus.register(coljersey);
    bus.register(DatasetInfoCache.CACHE);
    if (cfg.apiURI != null) {
      bus.register(new CacheFlush(httpClient, cfg.apiURI));
    }
    bus.register(new PublicReleaseListener(cfg, getSqlSessionFactory(), exdao, doiService, converter));
    bus.register(doiUpdater);
    bus.register(uCache);
  }

  @Override
  protected void onFatalError(Throwable t) {
    if (ni != null) {
      try {
        LOG.error("Fatal startup error, closing names index gracefully", t);
        ni.close();
      } catch (Exception e) {
        LOG.error("Failed to shutdown names index", e);
      }
    } else {
      LOG.error("Fatal startup error", t);
    }
    System.exit(1);
  }
  
  private void clearTmp(WsServerConfig cfg) {
    LOG.info("Clear scratch dir {}", cfg.normalizer.scratchDir);
    if (cfg.normalizer.scratchDir != null && cfg.normalizer.scratchDir.exists()) {
      try {
        FileUtils.cleanDirectory(cfg.normalizer.scratchDir);
      } catch (IOException e) {
        LOG.error("Error cleaning scratch dir {}", cfg.normalizer.scratchDir, e);
      }
    }
  }
  
  /**
   * Mostly copied from HttpClientBuilder
   */
  public static RequestConfig requestConfig(JerseyClientConfiguration cfg) {
    final String cookiePolicy =
        cfg.isCookiesEnabled() ? CookieSpecs.DEFAULT : CookieSpecs.IGNORE_COOKIES;
    return RequestConfig.custom()
        .setCookieSpec(cookiePolicy)
        .setSocketTimeout((int) cfg.getTimeout().toMilliseconds())
        .setConnectTimeout((int) cfg.getConnectionTimeout().toMilliseconds())
        .setConnectionRequestTimeout((int) cfg.getConnectionRequestTimeout().toMilliseconds())
        .build();
  }
}
