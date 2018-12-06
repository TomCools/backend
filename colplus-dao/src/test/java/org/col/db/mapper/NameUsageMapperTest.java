package org.col.db.mapper;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.col.api.model.Synonym;
import org.col.api.model.VernacularName;
import org.col.api.search.NameUsageWrapper;
import org.junit.Assert;
import org.junit.Test;

import static org.col.api.TestEntityGenerator.NAME4;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class NameUsageMapperTest extends MapperTestBase<NameUsageMapper> {
  
  public NameUsageMapperTest() {
    super(NameUsageMapper.class);
  }
  
  private AtomicInteger counter = new AtomicInteger(0);
  
  @Test
  public void processDatasetTaxa() throws Exception {
    mapper().processDatasetTaxa(NAME4.getDatasetKey(), new ResultHandler<NameUsageWrapper>() {
      public void handleResult(ResultContext<? extends NameUsageWrapper> ctx) {
        counter.incrementAndGet();
        for (VernacularName v : ctx.getResultObject().getVernacularNames()) {
          assertNotNull(v.getName());
        }
      }
    });
    Assert.assertEquals(2, counter.get());
  }
  
  @Test
  public void processDatasetSynonyms() throws Exception {
    mapper().processDatasetSynonyms(NAME4.getDatasetKey(), new ResultHandler<NameUsageWrapper>() {
      public void handleResult(ResultContext<? extends NameUsageWrapper> ctx) {
        counter.incrementAndGet();
        assertTrue(ctx.getResultObject().getUsage().getStatus().isSynonym());
        assertNotNull(((Synonym)ctx.getResultObject().getUsage()).getAccepted());
      }
    });
    Assert.assertEquals(2, counter.get());
  }
  
  @Test
  public void processDatasetBareNames() throws Exception {
    mapper().processDatasetBareNames(NAME4.getDatasetKey(), new ResultHandler<NameUsageWrapper>() {
      public void handleResult(ResultContext<? extends NameUsageWrapper> ctx) {
        counter.incrementAndGet();
        assertNotNull(ctx.getResultObject());
        assertNotNull(ctx.getResultObject().getUsage());
        assertNotNull(ctx.getResultObject().getUsage().getName());
      }
    });
    Assert.assertEquals(1, counter.get());
  }
}
