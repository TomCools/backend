package life.catalogue.parser;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

/**
 *
 */
public class BooleanParserTest extends ParserTestBase<Boolean> {

  public BooleanParserTest() {
    super(BooleanParser.PARSER);
  }

  @Test
  public void parse() throws Exception {
    assertParse(true, "true");
    assertParse(true, "yes");
    assertParse(true, " t    ");
    assertParse(true, "T");
    assertParse(true, "si");
    assertParse(true, "ja");
    assertParse(true, "oui");
    assertParse(true, "wahr");
    assertParse(true, "1");
  
    assertParse(false, "0");
    assertParse(false, "f");
    assertParse(false, "f");
    assertParse(false, "no");
    assertParse(false, "nein");
    assertParse(false, "-1");
  }

  @Override
  List<String> additionalUnparsableValues() {
    return Lists.newArrayList("t ur e", "a", "2");
  }

}