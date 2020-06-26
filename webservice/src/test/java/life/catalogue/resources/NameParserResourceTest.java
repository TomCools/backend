package life.catalogue.resources;

import life.catalogue.api.model.Name;
import life.catalogue.api.model.ParsedNameUsage;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.NomCode;
import org.gbif.nameparser.api.Rank;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import java.util.List;

import static life.catalogue.ApiUtils.userCreds;
import static org.junit.Assert.assertEquals;

public class NameParserResourceTest extends ResourceTestBase {

  GenericType<List<ParsedNameUsage>> PARSER_TYPE = new GenericType<List<ParsedNameUsage>>() {
  };

  public NameParserResourceTest() {
    super("/parser/name");
  }

  @Test
  public void parseGet() {
    List<ParsedNameUsage> resp = userCreds(base.queryParam("name", "Abies alba Mill.")
                                               .queryParam("code", "botanical")
    ).get(PARSER_TYPE);
    
    Name abies = new Name();
    abies.setGenus("Abies");
    abies.setSpecificEpithet("alba");
    abies.getCombinationAuthorship().getAuthors().add("Mill.");
    abies.setType(NameType.SCIENTIFIC);
    abies.setRank(Rank.SPECIES);
    abies.setCode(NomCode.BOTANICAL);
    abies.rebuildScientificName();
    abies.rebuildAuthorship();
    
    assertEquals(1, resp.size());
    //printDiff(abies, resp.get(0).getName());
    assertEquals(abies, resp.get(0).getName());
  }
}