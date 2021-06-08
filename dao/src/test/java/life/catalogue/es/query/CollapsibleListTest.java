package life.catalogue.es.query;

import life.catalogue.es.EsModule;

import java.util.ArrayList;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CollapsibleListTest {

  /**
   * @throws JsonProcessingException
   */
  @Test
  public void test1() throws JsonProcessingException {
    CollapsibleList<String> list = new CollapsibleList<>();
    Object obj = list.collapse();
    assertNull(obj);
    System.out.println(EsModule.writeDebug(list));
  }

  @Test
  public void test2() {
    CollapsibleList<String> list = new CollapsibleList<>();
    list.add("one");
    Object obj = list.collapse();
    assertEquals(String.class, obj.getClass());
    System.out.println(EsModule.writeDebug(list));
  }

  @Test
  public void test3() {
    CollapsibleList<String> list = new CollapsibleList<>();
    list.add("one");
    list.add("two");
    list.add("three");
    Object obj = list.collapse();
    assertEquals(ArrayList.class, obj.getClass());
    System.out.println(EsModule.writeDebug(list));
  }

}
