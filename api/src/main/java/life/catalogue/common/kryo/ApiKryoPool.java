package life.catalogue.common.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.Pool;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.undercouch.citeproc.csl.CSLType;

import life.catalogue.api.datapackage.ColdpTerm;
import life.catalogue.api.model.*;
import life.catalogue.api.search.NameUsageWrapper;
import life.catalogue.api.search.SimpleDecision;
import life.catalogue.api.vocab.*;
import life.catalogue.common.date.FuzzyDate;
import life.catalogue.common.kryo.jdk.JdkImmutableListSerializer;
import life.catalogue.common.kryo.jdk.JdkImmutableMapSerializer;
import life.catalogue.common.kryo.jdk.JdkImmutableSetSerializer;
import org.gbif.dwc.terms.BibTexTerm;
import org.gbif.dwc.terms.TermFactory;
import org.gbif.dwc.terms.UnknownTerm;
import org.gbif.nameparser.api.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


/**
 * Creates a kryo factory usable for thread safe kryo pools that can deal with clb api classes.
 * We use Kryo for extremely fast byte serialization of temporary objects.
 * It is used to serialize various information in kvp stores during checklist indexing and nub builds.
 */
public class ApiKryoPool extends Pool<Kryo> {

  public ApiKryoPool(int maximumCapacity) {
    super(true, true, maximumCapacity);
  }

  @Override
  public Kryo create() {
    return configure(new Kryo());
  }

  public static Kryo configure(Kryo kryo) {
    kryo.setRegistrationRequired(true);

    // col core
    kryo.register(Agent.class);
    kryo.register(Authorship.class);
    kryo.register(BareName.class);
    kryo.register(Citation.class);
    kryo.register(Classification.class);
    kryo.register(Dataset.class);
    kryo.register(DatasetImport.class);
    kryo.register(DOI.class);
    kryo.register(Distribution.class);
    kryo.register(EditorialDecision.class);
    kryo.register(EditorialDecision.Mode.class);
    kryo.register(IndexName.class);
    kryo.register(Media.class);
    kryo.register(Name.class);
    kryo.register(ParsedNameUsage.class);
    kryo.register(NameRelation.class);
    kryo.register(Page.class);
    kryo.register(ParsedName.class);
    kryo.register(ParsedName.State.class);
    kryo.register(Reference.class);
    kryo.register(Sector.class);
    kryo.register(Sector.Mode.class);
    kryo.register(SimpleDecision.class);
    kryo.register(SimpleName.class);
    kryo.register(SpeciesEstimate.class);
    kryo.register(SpeciesInteraction.class);
    kryo.register(Synonym.class);
    kryo.register(Taxon.class);
    kryo.register(TaxonConceptRelation.class);
    kryo.register(Treatment.class);
    kryo.register(TypeMaterial.class);
    kryo.register(VerbatimRecord.class);
    kryo.register(VernacularName.class);

    // search
    kryo.register(NameUsageWrapper.class);
    kryo.register(SimpleDecision.class);

    // CSL classes & enums
    kryo.register(CslData.class);
    kryo.register(CslDate.class);
    kryo.register(CslName.class);
    kryo.register(CslName[].class);
    kryo.register(CSLType.class);
    kryo.register(int[][].class);
    kryo.register(String[].class);

    // date/time
    kryo.register(FuzzyDate.class, new FuzzyDateSerializer());
    kryo.register(LocalDate.class);
    kryo.register(LocalDateTime.class);

    // java & commons
    kryo.register(ArrayList.class);
    kryo.register(HashMap.class);
    kryo.register(HashSet.class);
    kryo.register(int[].class);
    kryo.register(LinkedHashMap.class);
    kryo.register(LinkedList.class);
    kryo.register(URI.class, new URISerializer());
    kryo.register(UUID.class, new UUIDSerializer());
    // private class, special registration
    try {
      Class clazz = Class.forName("java.util.Arrays$ArrayList");
      kryo.register(clazz);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    // guave collections
    UnmodifiableCollectionsSerializer.registerSerializers( kryo );
    // pre java9 immutable collections
    ImmutableListSerializer.registerSerializers(kryo);
    // java9 immutable collections
    JdkImmutableListSerializer.registerSerializers(kryo);
    JdkImmutableMapSerializer.registerSerializers(kryo);
    JdkImmutableSetSerializer.registerSerializers(kryo);
    // fastutils
    FastUtilsSerializer.registerSerializers(kryo, true);

    // enums
    kryo.register(Country.class);
    kryo.register(DataFormat.class);
    kryo.register(DatasetOrigin.class);
    kryo.register(Setting.class);
    kryo.register(DatasetType.class);
    kryo.register(DistributionStatus.class);
    kryo.register(EnumMap.class, new EnumMapSerializer());
    kryo.register(EnumSet.class, new EnumSetSerializer());
    kryo.register(EstimateType.class);
    kryo.register(Frequency.class);
    kryo.register(Gazetteer.class);
    kryo.register(ImportState.class);
    kryo.register(Issue.class);
    kryo.register(JobStatus.class);
    kryo.register(Kingdom.class);
    kryo.register(License.class);
    kryo.register(Environment.class);
    kryo.register(MatchType.class);
    kryo.register(MediaType.class);
    kryo.register(NamePart.class);
    kryo.register(NameType.class);
    kryo.register(NomCode.class);
    kryo.register(NomRelType.class);
    kryo.register(NomStatus.class);
    kryo.register(Origin.class);
    kryo.register(Rank.class);
    kryo.register(Sex.class);
    kryo.register(SpeciesInteractionType.class);
    kryo.register(TaxonomicStatus.class);
    kryo.register(TaxonConceptRelType.class);
    kryo.register(TreatmentFormat.class);
    kryo.register(TypeStatus.class);

    // term enums
    TermFactory.instance().registerTermEnum(ColdpTerm.class);
    TermFactory.instance().registerTermEnum(ColDwcTerm.class);
    TermFactory.instance().registerTermEnum(TxtTreeTerm.class);
    for (Class cl : TermFactory.instance().listRegisteredTermEnums()) {
      kryo.register(cl);
    }
    kryo.register(UnknownTerm.class, new TermSerializer());
    kryo.register(BibTexTerm.class, new TermSerializer());
    return kryo;
  }
}
