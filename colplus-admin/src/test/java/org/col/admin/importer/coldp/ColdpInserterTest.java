package org.col.admin.importer.coldp;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.col.api.model.Dataset;
import org.col.api.vocab.DataFormat;
import org.col.api.vocab.DatasetType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColdpInserterTest {
  
  @Test
  public void readMetadata() throws Exception {
    URL url = getClass().getResource("/coldp/0");
    Path coldp0 = Paths.get(url.toURI());
  
    //TermFactory.instance().registerTermEnum(ColTerm.class);

    ColdpInserter ins = new ColdpInserter(null, coldp0, null);
    Dataset d = ins.readMetadata().get();
    
    assertEquals("The full dataset title", d.getTitle());
    assertEquals(DatasetType.OTHER, d.getType());
    assertEquals("ver. (06/2018)", d.getVersion());
    assertEquals("Froese R. & Pauly D. (eds) (2018). FishBase (version 06/2018).", d.getCitation());
    assertEquals("https://www.fishbase.de/images/gifs/fblogo_new.gif", d.getLogo().toString());
    assertEquals(DataFormat.COLDP, d.getDataFormat());
    
    assertEquals(3, d.getAuthorsAndEditors().size());
    assertEquals(10, d.getOrganisations().size());
  }
}