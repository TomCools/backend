package life.catalogue.matching;

import life.catalogue.api.TestEntityGenerator;
import life.catalogue.api.model.IndexName;
import org.junit.Test;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class NameIndexMapDBStoreTest {
  
  @Test
  public void size() throws Exception {
    File dbf = File.createTempFile("colNidxStore",".db");
    dbf.delete();
    try {
      DBMaker.Maker maker = DBMaker.fileDB(dbf).fileMmapEnableIfSupported();
      NameIndexMapDBStore db = new NameIndexMapDBStore(maker, dbf);
      db.start();
      assertEquals(0, db.count());

      db.put("a", newNameList(1));
      assertEquals(1, db.count());

      db.put("b", newNameList(2));
      assertEquals(3, db.count());

      db.put("c", newNameList(3));
      assertEquals(6, db.count());
  
      db.put("a", newNameList(3));
      assertEquals(8, db.count());

      // now shutdown and reopen
      db.stop();
      db = new NameIndexMapDBStore(maker, dbf);
      db.start();

      assertEquals(8, db.count());
  
      db.put("a", newNameList(2));
      assertEquals(7, db.count());
      
    } finally {
      dbf.delete();
    }
  }
  private ArrayList<IndexName> newNameList(int size) {
    ArrayList<IndexName> names = new ArrayList<>(size);
    for (int idx = 0; idx<size; idx++) {
      IndexName n = new IndexName(TestEntityGenerator.newName());
      n.setKey(idx);
      names.add(n);
    }
    return names;
  }

  @Test
  public void close() {
  }
}