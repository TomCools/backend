package org.col.es;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.col.api.jackson.ApiModule;
import org.col.api.model.NameUsage;
import org.col.common.lang.Exceptions;
import org.col.db.mapper.BatchResultHandler;
import org.col.db.mapper.NameUsageMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.col.es.EsConfig.DEFAULT_TYPE_NAME;
import static org.col.es.EsConfig.NAME_USAGE_BASE;

public class NameUsageIndexService {

  private static final Logger LOG = LoggerFactory.getLogger(NameUsageIndexService.class);
  private static final ObjectWriter WRITER = ApiModule.MAPPER.writerFor(NameUsage.class);

  private final RestClient client;
  private final EsConfig esConfig;
  private final SqlSessionFactory factory;
  /*
   * Asynchronous indexing is problematic if the rest client doesn't stick around long enough for
   * the callbacks to be invoked (as with unit tests).
   */
  private final boolean async;

  public NameUsageIndexService(RestClient client, EsConfig esConfig, SqlSessionFactory factory) {
    this(client, esConfig, factory, false);
  }

  @VisibleForTesting
  NameUsageIndexService(RestClient client, EsConfig esConfig, SqlSessionFactory factory,
      boolean async) {
    this.client = client;
    this.esConfig = esConfig;
    this.factory = factory;
    this.async = async;
  }

  /**
   * Main method to index an entire dataset from postgres into ElasticSearch using the bulk API.
   */
  public void indexDataset(final int datasetKey) throws EsException {
    final String index = NAME_USAGE_BASE + datasetKey;
    final int batchSize = esConfig.nameUsage.batchSize;
    EsUtil.deleteIndex(client, index);
    EsUtil.createIndex(client, index, esConfig.nameUsage);
    final AtomicInteger counter = new AtomicInteger();
    try (SqlSession session = factory.openSession()) {
      NameUsageMapper mapper = session.getMapper(NameUsageMapper.class);
      mapper.processDatasetBareNames(datasetKey, new BatchResultHandler<>(batch -> {
        indexBulk(index, batch);
        counter.addAndGet(batch.size());
      }, batchSize));
      mapper.processDatasetSynonyms(datasetKey, new BatchResultHandler<>(batch -> {
        indexBulk(index, batch);
        counter.addAndGet(batch.size());
      }, batchSize));
      mapper.processDatasetTaxa(datasetKey, new BatchResultHandler<>(batch -> {
        indexBulk(index, batch);
        counter.addAndGet(batch.size());
      }, batchSize));
    } catch (Exception e) {
      throw new EsException(e);
    } finally {
      EsUtil.refreshIndex(client, index);
    }
    LOG.info("Indexed {} name usages from dataset {} into index {}", counter.get(), datasetKey,
        index);
  }

  @VisibleForTesting
  void indexBulk(String index, List<? extends NameUsage> usages) {
    if(usages.size() == 0) {
      // This is actually probably amounts to an illegal state of affairs, but ok ...
      LOG.warn("Received empty batch of name usages while indexing into {}", index);
      return;
    }
    String actionMetaData = indexActionMetaData(index);
    StringBuilder body = new StringBuilder();
    try {
      for (NameUsage nu : usages) {
        body.append(actionMetaData);
        System.out.println(WRITER.writeValueAsString(nu));
        body.append(WRITER.writeValueAsString(nu));
        body.append("\n");
      }
      Request request = new Request("POST", "/_bulk");
      request.setJsonEntity(body.toString());
      if (async) {
        executeAsync(request, index, usages);
      } else {
        execute(request, index, usages);
      }
    } catch (Exception e) {
      Exceptions.throwRuntime(e);
    }
  }

  private void executeAsync(Request req, String index, List<? extends NameUsage> usages) {
    client.performRequestAsync(req, new ResponseListener() {

      @Override
      public void onSuccess(Response response) {
        LOG.debug("Successfully inserted {} name usages into index {}", usages.size(), index);
      }

      @Override
      public void onFailure(Exception e) {
        // No point in going on
        LOG.error("Error while populating index {}: {}", index, e.getMessage());
        Exceptions.throwRuntime(e);
      }
    });
  }

  private void execute(Request req, String index, List<? extends NameUsage> usages) {
    Response res;
    try {
      res = client.performRequest(req);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (res.getStatusLine().getStatusCode() == 200) {
      LOG.debug("Successfully inserted {} name usages into index {}", usages.size(), index);
    } else {
      String fmt = "Error while populating index %s: %s";
      String err = String.format(fmt, index, res.getStatusLine().getReasonPhrase());
      LOG.error(err);
      throw new RuntimeException(err);
    }
  }

  private static String indexActionMetaData(String index) {
    String fmt = "{ \"index\" : { \"_index\" : \"%s\", \"_type\" : \"%s\" } }%n";
    return String.format(fmt, index, DEFAULT_TYPE_NAME);
  }

}
