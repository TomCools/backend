package life.catalogue.common.tax;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class SciNameNormalizerTest {

  @Test
  public void testNormalize() throws Exception {
    assertEquals("", SciNameNormalizer.normalize(""));
    assertEquals("Abies", SciNameNormalizer.normalize("Abies "));
    assertEquals("Abiies", SciNameNormalizer.normalize("Abiies "));
    assertEquals("Abyes", SciNameNormalizer.normalize("Abyes "));
    assertEquals("Abyes alba", SciNameNormalizer.normalize("Abyes  albus"));
    assertEquals("Abyes albieta", SciNameNormalizer.normalize("Abyes albieta"));
    assertEquals("Abies albieta", SciNameNormalizer.normalize("Abies albijeta"));
    assertEquals("Abies albieta", SciNameNormalizer.normalize("Abies albyeta"));
    assertEquals("Abies alba", SciNameNormalizer.normalize(" \txAbies × ållbbus\t"));

    assertEquals("Abies alba", SciNameNormalizer.normalize(" \txAbies × ållbbus\t"));
    assertEquals("Rhachis takta", SciNameNormalizer.normalize("Rhachis taktos"));

    assertEquals("Hieracium sabauda", SciNameNormalizer.normalize("Hieracium sabaudum"));
    assertEquals("Hieracium scorzoneraefolia", SciNameNormalizer.normalize("Hieracium scorzoneræfolium"));
    assertEquals("Hieracium scorzonerifolia", SciNameNormalizer.normalize("Hieracium scorzonerifolium"));
    assertEquals("Macrozamia platiracha", SciNameNormalizer.normalize("Macrozamia platyrachis"));
    assertEquals("Macrozamia platiracha", SciNameNormalizer.normalize("Macrozamia platyrhachis"));
    assertEquals("Cycas circinala", SciNameNormalizer.normalize("Cycas circinalis"));
    assertEquals("Cycas circinala", SciNameNormalizer.normalize("Cycas circinnalis"));
    assertEquals("Isolona periera", SciNameNormalizer.normalize("Isolona perrieri"));
    assertEquals("Isolona periera", SciNameNormalizer.normalize("Isolona perrierii"));

    assertEquals("Carex caioueta", SciNameNormalizer.normalize("Carex ×cayouettei"));
    assertEquals("Platanus hispanica", SciNameNormalizer.normalize("Platanus x hispanica"));
    // https://github.com/gbif/checklistbank/issues/7
    assertEquals(SciNameNormalizer.normalize("Eragrostis brownii"), SciNameNormalizer.normalize("Eragrostis brownei"));
    assertEquals("Eragrostis browna", SciNameNormalizer.normalize("Eragrostis brownei"));
    assertEquals("Theridion uhliga", SciNameNormalizer.normalize("Theridion uhlighi"));
    assertEquals("Theridion uhliga", SciNameNormalizer.normalize("Theridion uhliigi"));

    assertEquals("Lynx rufus baila", SciNameNormalizer.normalize("Lynx rufus baileii"));
    assertEquals("Lynx rufus baila", SciNameNormalizer.normalize("Lynx rufus baileyi"));
  }

  @Test
  public void testNormalizeAll() throws Exception {
    assertEquals("", SciNameNormalizer.normalizeAll(""));
    assertEquals("Abies", SciNameNormalizer.normalizeAll("Abies "));
    assertEquals("Abies", SciNameNormalizer.normalizeAll("Abiies "));
    assertEquals("Abies", SciNameNormalizer.normalizeAll("Abyes "));
    assertEquals("Abies alba", SciNameNormalizer.normalizeAll("Abyes  albus"));
    assertEquals("Abies albieta", SciNameNormalizer.normalizeAll("Abyes albieta"));
    assertEquals("Abies albieta", SciNameNormalizer.normalizeAll("Abies albijeta"));
    assertEquals("Abies albieta", SciNameNormalizer.normalizeAll("Abies albyeta"));
    assertEquals("Abies alba", SciNameNormalizer.normalizeAll(" \txAbies × ållbbus\t"));

    assertEquals("Abies alba", SciNameNormalizer.normalizeAll(" \txAbies × ållbbus\t"));
    assertEquals("Rachis takta", SciNameNormalizer.normalizeAll("Rhachis taktos"));

    assertEquals("Hieracium sabauda", SciNameNormalizer.normalizeAll("Hieracium sabaudum"));
    assertEquals("Hieracium scorzoneraefolia", SciNameNormalizer.normalizeAll("Hieracium scorzoneræfolium"));
    assertEquals("Hieracium scorzonerifolia", SciNameNormalizer.normalizeAll("Hieracium scorzonerifolium"));
    assertEquals("Macrozamia platiracha", SciNameNormalizer.normalizeAll("Macrozamia platyrachis"));
    assertEquals("Macrozamia platiracha", SciNameNormalizer.normalizeAll("Macrozamia platyrhachis"));
    assertEquals("Cicas circinala", SciNameNormalizer.normalizeAll("Cycas circinalis"));
    assertEquals("Cicas circinala", SciNameNormalizer.normalizeAll("Cycas circinnalis"));
    assertEquals("Isolona periera", SciNameNormalizer.normalizeAll("Isolona perrieri"));
    assertEquals("Isolona periera", SciNameNormalizer.normalizeAll("Isolona perrierii"));

    assertEquals("Carex caioueta", SciNameNormalizer.normalizeAll("Carex ×cayouettei"));
    assertEquals("Platanus hispanica", SciNameNormalizer.normalizeAll("Platanus x hispanica"));
    // https://github.com/gbif/checklistbank/issues/7
    assertEquals("Eragrostis browna", SciNameNormalizer.normalizeAll("Eragrostis brownii"));
    assertEquals("Eragrostis browna", SciNameNormalizer.normalizeAll("Eragrostis brownei"));
  }

  @Test
  public void testHybridCross() throws Exception {
    assertEquals("xcayouettei", SciNameNormalizer.normalize("xcayouettei"));
    assertEquals("cayouettei", SciNameNormalizer.normalize("×cayouettei"));

    assertEquals("Carex xcaioueta", SciNameNormalizer.normalize("Carex xcayouettei"));
    assertEquals("Carex caioueta", SciNameNormalizer.normalize("Carex ×cayouettei"));
    assertEquals("Carex caioueta", SciNameNormalizer.normalize("×Carex cayouettei"));
    assertEquals("Carex xcaioueta", SciNameNormalizer.normalize("xCarex xcayouettei"));
    assertEquals("Carex caioueta", SciNameNormalizer.normalize("XCarex cayouettei"));
    assertEquals("Carex caioueta", SciNameNormalizer.normalize("xCarex ×cayouettei"));

    assertEquals("Platanus hispanica", SciNameNormalizer.normalize("Platanus x hispanica"));

  }

  @Test
  public void testNonAscii() throws Exception {
    assertEquals("Cem Andrexa", SciNameNormalizer.normalize("Çem Ándrexï"));
    assertEquals("SOEZsoezY¥µAAAAAAAECEEEEIIIIDNOOOOOOUUUUYssaaaaaaaeceeeeiiiidnoooooouuuuyy", SciNameNormalizer.normalize("ŠŒŽšœžŸ¥µÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýÿ"));
  }

  @Test
  public void testEpithetStemming() throws Exception {
    assertEquals("", SciNameNormalizer.stemEpithet(""));

    assertEquals("alba", SciNameNormalizer.stemEpithet("alba"));
    assertEquals("alba", SciNameNormalizer.stemEpithet("albus"));
    assertEquals("alba", SciNameNormalizer.stemEpithet("album"));

    assertEquals("dentata", SciNameNormalizer.stemEpithet("dentata"));
    assertEquals("dentata", SciNameNormalizer.stemEpithet("dentatus"));
    assertEquals("dentata", SciNameNormalizer.stemEpithet("dentatum"));

    assertEquals("multa", SciNameNormalizer.stemEpithet("multi"));
    assertEquals("multa", SciNameNormalizer.stemEpithet("multae"));
    assertEquals("multa", SciNameNormalizer.stemEpithet("multa"));

    assertEquals("facila", SciNameNormalizer.stemEpithet("facilis"));
    assertEquals("facila", SciNameNormalizer.stemEpithet("facile"));

    assertEquals("dulca", SciNameNormalizer.stemEpithet("dulcis"));
    assertEquals("dulca", SciNameNormalizer.stemEpithet("dulce"));

    assertEquals("virida", SciNameNormalizer.stemEpithet("viridis"));
    assertEquals("virida", SciNameNormalizer.stemEpithet("viride"));

    assertEquals("capensa", SciNameNormalizer.stemEpithet("capensis"));
    assertEquals("capensa", SciNameNormalizer.stemEpithet("capense"));

    assertEquals("lanigra", SciNameNormalizer.stemEpithet("laniger"));
    assertEquals("lanigra", SciNameNormalizer.stemEpithet("lanigera"));
    assertEquals("lanigra", SciNameNormalizer.stemEpithet("lanigerum"));

    assertEquals("cerifra", SciNameNormalizer.stemEpithet("cerifer"));
    assertEquals("cerifra", SciNameNormalizer.stemEpithet("cerifera"));
    assertEquals("cerifra", SciNameNormalizer.stemEpithet("ceriferum"));

    assertEquals("rubra", SciNameNormalizer.stemEpithet("ruber"));
    assertEquals("rubra", SciNameNormalizer.stemEpithet("rubra"));

    assertEquals("porifra", SciNameNormalizer.stemEpithet("porifera"));
    assertEquals("porifra", SciNameNormalizer.stemEpithet("porifer"));
    assertEquals("porifra", SciNameNormalizer.stemEpithet("poriferum"));

    assertEquals("nigra", SciNameNormalizer.stemEpithet("niger"));
    assertEquals("nigra", SciNameNormalizer.stemEpithet("nigra"));
    assertEquals("nigra", SciNameNormalizer.stemEpithet("nigrum"));

    assertEquals("pulchra", SciNameNormalizer.stemEpithet("pulcher")); // should we stem cher too?
    assertEquals("pulchra", SciNameNormalizer.stemEpithet("pulchra"));
    assertEquals("pulchra", SciNameNormalizer.stemEpithet("pulchrum"));

    assertEquals("muliebra", SciNameNormalizer.stemEpithet("muliebris"));
    assertEquals("muliebra", SciNameNormalizer.stemEpithet("muliebre"));

    assertEquals("allba", SciNameNormalizer.stemEpithet("allbus"));
    assertEquals("alaba", SciNameNormalizer.stemEpithet("alaba"));
    assertEquals("alaa", SciNameNormalizer.stemEpithet("alaus"));
    assertEquals("alaa", SciNameNormalizer.stemEpithet("alaa"));

    assertEquals("pericula", SciNameNormalizer.stemEpithet("periculum"));
    assertEquals("pericula", SciNameNormalizer.stemEpithet("periculi"));
    assertEquals("pericula", SciNameNormalizer.stemEpithet("pericula"));

    assertEquals("viator", SciNameNormalizer.stemEpithet("viatrix"));
    assertEquals("viator", SciNameNormalizer.stemEpithet("viator"));
    assertEquals("viatora", SciNameNormalizer.stemEpithet("viatoris"));

    assertEquals("nevadensa", SciNameNormalizer.stemEpithet("nevadense"));
    assertEquals("nevadensa", SciNameNormalizer.stemEpithet("nevadensis"));

    assertEquals("orientala", SciNameNormalizer.stemEpithet("orientale"));
    assertEquals("orientala", SciNameNormalizer.stemEpithet("orientalis"));

    // UNCHANGED
    assertEquals("ferox", SciNameNormalizer.stemEpithet("ferox"));
    assertEquals("simplex", SciNameNormalizer.stemEpithet("simplex"));
    assertEquals("elegans", SciNameNormalizer.stemEpithet("elegans"));
    assertEquals("pubescens", SciNameNormalizer.stemEpithet("pubescens"));
    // greek names stay as they are
    assertEquals("albon", SciNameNormalizer.stemEpithet("albon"));
  }
}
