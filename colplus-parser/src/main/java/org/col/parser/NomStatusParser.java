package org.col.parser;

import org.col.api.vocab.NomStatus;

/**
 *
 */
public class NomStatusParser extends EnumParser<NomStatus> {
  public static final NomStatusParser PARSER = new NomStatusParser();
  
  public NomStatusParser() {
    super("nomstatus.csv", NomStatus.class);
    for (NomStatus st : NomStatus.values()) {
      add(st.getBotanicalLabel(), st);
      add(st.getZoologicalLabel(), st);
      add(st.getAbbreviatedLabel(), st);
    }
  }
  
}
