package life.catalogue.assembly;

import life.catalogue.api.model.Name;
import life.catalogue.api.model.NameUsageBase;

import life.catalogue.api.model.Taxon;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParentStackTest {

  @Test
  public void testStack() throws Exception {
    NameUsageBase king = new Taxon();
    king.setName(new Name());
    ParentStack parents = new ParentStack(king);

    assertEquals(0, parents.size());
    assertNull(parents.last());
    assertEquals(king, parents.lowestParentMatch());

    parents.put(src(1, null));
    parents.put(src(2, 1));
    var nub = match("nub#3");
    parents.setMatch(nub);
    assertEquals(nub, parents.lowestParentMatch());
    assertEquals(2, parents.size());

    assertFalse(parents.isDoubtful());
    parents.markSubtreeAsDoubtful(); // doubtful key=2
    assertTrue(parents.isDoubtful());

    parents.put(src(3, 2));
    assertEquals(3, parents.size());
    assertEquals(nub, parents.lowestParentMatch());
    assertTrue(parents.isDoubtful());

    parents.put(src(4, 1)); // this removes all but the first key
    assertEquals(2, parents.size());
    assertFalse(parents.isDoubtful());
    assertNotNull(parents.last());
  }

  private NameUsageBase src(int key, Integer parentKey) {
    NameUsageBase u = new Taxon();
    u.setId(String.valueOf(key));
    u.setParentId(parentKey == null ? null : String.valueOf(parentKey));
    u.setName(new Name());
    u.getName().setScientificName("Sciname #" + key);
    return u;
  }

  private NameUsageBase match(String name) {
    NameUsageBase n = new Taxon();
    n.setName(new Name());
    n.getName().setScientificName(name);
    return n;
  }

}