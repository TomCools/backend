package org.col.db.mapper;

import org.col.api.TestEntityGenerator;
import org.col.api.model.DatasetIDEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

abstract class DatasetCRUDTest<T extends DatasetIDEntity, M extends DatasetCRUDMapper<T>> extends MapperTestBase<M> {
  
  private final int datasetKey;
  
  public DatasetCRUDTest(Class<M> mapperClazz) {
    super(mapperClazz);
    datasetKey = TestEntityGenerator.DATASET11.getKey();
  }
  
  abstract T createTestEntity();
  
  abstract T removeDbCreatedProps(T obj);
  
  @Test
  public void getNull() throws Exception {
    assertNull(mapper().get(datasetKey, null));
    assertNull(mapper().get(datasetKey, ""));
  }
  
  @Test
  public void roundtrip() throws Exception {
    T u1 = createTestEntity();
    mapper().create(u1);
    commit();
    
    removeDbCreatedProps(u1);
    T u2 = removeDbCreatedProps(mapper().get(datasetKey, u1.getId()));
    printDiff(u1, u2);
    assertEquals(u1, u2);
  }
  
  abstract void updateTestObj(T obj);
  
  @Test
  public void update() throws Exception {
    T u1 = createTestEntity();
    mapper().create(u1);
    commit();
    
    updateTestObj(u1);
    mapper().update(u1);
    commit();
  
    removeDbCreatedProps(u1);
    T u2 = removeDbCreatedProps(mapper().get(datasetKey, u1.getId()));
  
    printDiff(u1, u2);
    assertEquals(u1, u2);
  }
  
  @Test
  public void deleted() throws Exception {
    T u1 = createTestEntity();
    mapper().create(u1);
    commit();
    
    mapper().delete(datasetKey, u1.getId());
    commit();
    
    assertNull(mapper().get(datasetKey, u1.getId()));
  }
  
}