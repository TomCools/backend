package life.catalogue.importer.coldp;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import life.catalogue.api.model.*;
import life.catalogue.api.util.ObjectUtils;
import life.catalogue.api.vocab.*;
import life.catalogue.coldp.ColdpTerm;
import life.catalogue.importer.InterpreterBase;
import life.catalogue.importer.MappingFlags;
import life.catalogue.importer.neo.NeoDb;
import life.catalogue.importer.neo.model.NeoName;
import life.catalogue.importer.neo.model.NeoRel;
import life.catalogue.importer.neo.model.NeoUsage;
import life.catalogue.importer.neo.model.RelType;
import life.catalogue.importer.reference.ReferenceFactory;
import life.catalogue.parser.*;
import org.gbif.dwc.terms.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static life.catalogue.parser.SafeParser.parse;

/**
 * Interprets a verbatim ACEF record and transforms it into a name, taxon and unique references.
 */
public class ColdpInterpreter extends InterpreterBase {
  private static final Logger LOG = LoggerFactory.getLogger(ColdpInterpreter.class);
  private static final EnumNote<TaxonomicStatus> SYN_NOTE = new EnumNote<>(TaxonomicStatus.SYNONYM, null);
  private static final EnumNote<TaxonomicStatus> ACC_NOTE = new EnumNote<>(TaxonomicStatus.ACCEPTED, null);
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings(); // for multi value ID fields

  ColdpInterpreter(DatasetSettings settings, MappingFlags metadata, ReferenceFactory refFactory, NeoDb store) {
    super(settings, refFactory, store);
    // turn on normalization of flat classification
    metadata.setDenormedClassificationMapped(true);
  }

  Optional<Reference> interpretReference(VerbatimRecord v) {
    if (!v.hasTerm(ColdpTerm.ID)) {
      return Optional.empty();
    }
    return Optional.of(refFactory.fromColDP(v));
  }

  Optional<NeoUsage> interpretNameUsage(VerbatimRecord v) {
    // name
    return interpretName(v).map(nn -> {
      if (!v.hasTerm(ColdpTerm.ID)) {
        return null;
      }

      NeoUsage u;
      TaxonomicStatus status = parse(TaxonomicStatusParser.PARSER, v.get(ColdpTerm.status)).orElse(ACC_NOTE, Issue.TAXONOMIC_STATUS_INVALID, v).val;
      if (status.isBareName()) {
        u = NeoUsage.createBareName(Origin.SOURCE);
      } else if (status.isSynonym()) {
        u = NeoUsage.createSynonym(Origin.SOURCE, status);
      } else {
        u = NeoUsage.createTaxon(Origin.SOURCE, status);
        interpretTaxonInfos(u, nn, v);
      }
      interpretUsageBase(u, nn, v);
      return u;
    });
  }

  Optional<NeoUsage> interpretTaxon(VerbatimRecord v) {
    return findName(v, ColdpTerm.nameID).map(n -> {
      if (!v.hasTerm(ColdpTerm.ID)) {
        return null;
      }
      NeoUsage u = NeoUsage.createTaxon(Origin.SOURCE, TaxonomicStatus.ACCEPTED);

      // shared usage base
      interpretUsageBase(u, n, v);

      // taxon
      interpretTaxonInfos(u, n, v);

      return u;
    });
  }

  private void interpretTaxonInfos(NeoUsage u, NeoName n, VerbatimRecord v){
    if (!u.isSynonym()) {
      Taxon t = u.asTaxon();
      t.setScrutinizer(v.get(ColdpTerm.scrutinizer));
      t.setScrutinizerDate(fuzzydate(v, Issue.SCRUTINIZER_DATE_INVALID, ColdpTerm.scrutinizerDate));
      if (v.hasTerm(ColdpTerm.extinct)) {
        t.setExtinct(bool(v, Issue.IS_EXTINCT_INVALID, ColdpTerm.extinct));
      } else {
        t.setExtinct(n.pnu.isExtinct());
      }
      // geotime
      t.setTemporalRangeStart(parse(GeoTimeParser.PARSER, v.get(ColdpTerm.temporalRangeStart)).orNull(Issue.GEOTIME_INVALID, v));
      t.setTemporalRangeEnd(parse(GeoTimeParser.PARSER, v.get(ColdpTerm.temporalRangeEnd)).orNull(Issue.GEOTIME_INVALID, v));
      // status
      if (Objects.equals(Boolean.TRUE, bool(v, Issue.PROVISIONAL_STATUS_INVALID, ColdpTerm.provisional))) {
        t.setStatus(TaxonomicStatus.PROVISIONALLY_ACCEPTED);
      }
      // environment
      setEnvironment(t, v, ColdpTerm.environment);
    }
    // flat classification for any usage
    u.classification = interpretClassification(v);
  }

  Optional<NeoUsage> interpretSynonym(VerbatimRecord v) {
    return findName(v, ColdpTerm.nameID).map(n -> {
      TaxonomicStatus status = parse(TaxonomicStatusParser.PARSER, v.get(ColdpTerm.status)).orElse(SYN_NOTE).val;
      if (!status.isSynonym()) {
        v.addIssue(Issue.TAXONOMIC_STATUS_INVALID);
        // override status as we require some accepted status on Taxon and some synonym status for
        status = TaxonomicStatus.SYNONYM;
      }
  
      NeoUsage u = NeoUsage.createSynonym(Origin.SOURCE, status);
      interpretUsageBase(u, n, v);
      if (!v.hasTerm(ColdpTerm.ID)) {
        u.setId(v.getRaw(ColdpTerm.taxonID) + "-" + v.getRaw(ColdpTerm.nameID));
      }
      return u;
    });
  }

  private Optional<NeoName> findName(VerbatimRecord v, Term nameId) {
    NeoName n = store.names().objByID(v.getRaw(nameId));
    if (n == null) {
      v.addIssue(Issue.NAME_ID_INVALID);
      v.addIssue(Issue.NOT_INTERPRETED);
      return Optional.empty();
    }
    return Optional.of(n);
  }

  private void interpretUsageBase(NeoUsage u, NeoName n, VerbatimRecord v) {
    u.nameNode = n.node;
    u.setId(v.getRaw(ColdpTerm.ID));
    u.setVerbatimKey(v.getId());
    setReference(v, ColdpTerm.accordingToID, u.usage::setAccordingToId, u.usage::setAccordingTo);
    u.usage.setOrigin(Origin.SOURCE);
    u.usage.setNamePhrase(ObjectUtils.coalesce(v.get(ColdpTerm.namePhrase), n.pnu.getTaxonomicNote()));
    u.usage.setRemarks(v.get(ColdpTerm.remarks));
    if (!u.usage.isBareName()) {
      NameUsageBase nub = (NameUsageBase) u.usage;
      setReferences(v, ColdpTerm.referenceID, COMMA_SPLITTER, nub::setReferenceIds);
      nub.setLink(uri(v, Issue.URL_INVALID, ColdpTerm.link));
    }

    u.usage.setName(n.getName());
  }

  Optional<NeoRel> interpretNameRelations(VerbatimRecord rec) {
    return interpretRelations(rec, NomRelTypeParser.PARSER, RelType::from);
  }

  Optional<NeoRel> interpretTaxonRelations(VerbatimRecord rec) {
    return interpretRelations(rec, TaxonConceptRelTypeParser.PARSER, RelType::from);
  }

  Optional<NeoRel> interpretSpeciesInteractions(VerbatimRecord rec) {
    return interpretRelations(rec, SpeciesInteractionTypeParser.PARSER, RelType::from);
  }

  <T extends Enum> Optional<NeoRel> interpretRelations(VerbatimRecord rec, EnumParser<T> parser, Function<T, RelType> typeFunction) {
    NeoRel rel = new NeoRel();
    SafeParser<T> type = SafeParser.parse(parser, rec.get(ColdpTerm.type));
    if (type.isPresent()) {
      rel.setType(typeFunction.apply(type.get()));
      rel.setRelatedScientificName(rec.get(ColdpTerm.relatedTaxonScientificName));
      rel.setRemarks(rec.get(ColdpTerm.remarks));
      setReference(rel, rec);
      return Optional.of(rel);
    }
    return Optional.empty();
  }

  Optional<TypeMaterial> interpretTypeMaterial(VerbatimRecord rec) {
    TypeMaterial m = new TypeMaterial();
    m.setId(rec.getRaw(ColdpTerm.ID));
    m.setNameId(rec.getRaw(ColdpTerm.nameID));
    m.setCitation(rec.get(ColdpTerm.citation));
    m.setStatus(SafeParser.parse(TypeStatusParser.PARSER, rec.get(ColdpTerm.status)).orElse(TypeStatus.OTHER, Issue.TYPE_STATUS_INVALID, rec));
    m.setLocality(rec.get(ColdpTerm.locality));
    m.setCountry(SafeParser.parse(CountryParser.PARSER, rec.get(ColdpTerm.country)).orNull(Issue.COUNTRY_INVALID, rec));
    try {
      Optional<CoordParser.LatLon> coord = CoordParser.PARSER.parse(rec.get(ColdpTerm.latitude), rec.get(ColdpTerm.longitude));
      if (coord.isPresent()) {
        m.setLatitude(coord.get().lat);
        m.setLongitude(coord.get().lon);
      }
    } catch (UnparsableException e) {
      rec.addIssue(Issue.LAT_LON_INVALID);
    }
    m.setAltitude(integer(rec, Issue.ALTITUDE_INVALID, ColdpTerm.altitude));
    m.setHost(rec.get(ColdpTerm.host));
    m.setDate(rec.get(ColdpTerm.date));
    m.setCollector(rec.get(ColdpTerm.collector));
    m.setLink(uri(rec, Issue.URL_INVALID, ColdpTerm.link));
    m.setRemarks(rec.get(ColdpTerm.remarks));
    setReference(m, rec);
    return Optional.of(m);
  }

  List<VernacularName> interpretVernacular(VerbatimRecord rec) {
    return super.interpretVernacular(rec,
        this::setReference,
        ColdpTerm.name,
        ColdpTerm.transliteration,
        ColdpTerm.language,
        ColdpTerm.sex,
        ColdpTerm.area,
        ColdpTerm.country
    );
  }

  List<Distribution> interpretDistribution(VerbatimRecord rec) {
    if (rec.hasTerm(ColdpTerm.areaID)) {
      return super.interpretDistributionByGazetteer(rec, this::setReference,
        ColdpTerm.areaID,
        ColdpTerm.gazetteer,
        ColdpTerm.status);

    } else if (rec.hasTerm(ColdpTerm.area)) {
      return createDistributions(Gazetteer.TEXT,
        rec.get(ColdpTerm.area),
        rec.get(ColdpTerm.status),
        rec, this::setReference
      );
    }
    return Collections.emptyList();
  }
  
  List<Media> interpretMedia(VerbatimRecord rec) {
    return interpretMedia(rec, this::setReference,
        ColdpTerm.type,
        ColdpTerm.url,
        ColdpTerm.link,
        ColdpTerm.license,
        ColdpTerm.creator,
        ColdpTerm.created,
        ColdpTerm.title,
        ColdpTerm.format);
  }

  public List<SpeciesEstimate> interpretEstimate(VerbatimRecord rec) {
    if (rec.hasTerm(ColdpTerm.estimate)) {
      Integer estimate = SafeParser.parse(IntegerParser.PARSER, rec.get(ColdpTerm.estimate)).orNull();
      if (estimate != null) {
        SpeciesEstimate est = new SpeciesEstimate();
        est.setEstimate(estimate);
        est.setVerbatimKey(rec.getId());
        est.setType(SafeParser.parse(EstimateTypeParser.PARSER, rec.get(ColdpTerm.type))
          .orElse(EstimateType.SPECIES_LIVING, Issue.ESTIMATE_TYPE_INVALID, rec));
        setReference(est, rec);
        est.setNote(rec.get(ColdpTerm.remarks));
        return Lists.newArrayList(est);

      } else {
        rec.addIssue(Issue.ESTIMATE_INVALID);
      }
    }
    return Collections.emptyList();
  }

  Optional<NeoName> interpretName(VerbatimRecord v) {
    Term nomStatusTerm = ColdpTerm.status;
    Term genusNameTerm = ColdpTerm.genus;
    Term remarksTerm = ColdpTerm.remarks;
    Term refIdTerm = ColdpTerm.referenceID;
    if (ColdpTerm.NameUsage.equals(v.getType())) {
      nomStatusTerm = ColdpTerm.nameStatus;
      genusNameTerm = ColdpTerm.genericName;
      remarksTerm = ColdpTerm.nameRemarks;
      refIdTerm = ColdpTerm.nameReferenceID;
    }

    Optional<ParsedNameUsage> opt = interpretName(true, v.get(ColdpTerm.ID),
        v.get(ColdpTerm.rank), v.get(ColdpTerm.scientificName), v.get(ColdpTerm.authorship),
        v.get(ColdpTerm.uninomial), v.get(genusNameTerm), v.get(ColdpTerm.infragenericEpithet), v.get(ColdpTerm.specificEpithet), v.get(ColdpTerm.infraspecificEpithet),
        v.get(ColdpTerm.cultivarEpithet),
        v.get(ColdpTerm.code), v.get(nomStatusTerm),
        v.get(ColdpTerm.link), v.get(remarksTerm), v);
    if (opt.isPresent()) {
      // publishedIn
      Name n = opt.get().getName();
      setReference(v, refIdTerm, rid -> {
          n.setPublishedInId(rid);
          n.setPublishedInPage(v.get(ColdpTerm.publishedInPage));
          n.setPublishedInYear(parseYear(ColdpTerm.publishedInYear, v));
      });
      if (opt.get().getPublishedIn() == null) {
        String pubInAuthorship = opt.get().getPublishedIn();
        if (n.getPublishedInId() == null) {
          setPublishedIn(n, pubInAuthorship, v);
        } else {
          //TODO: compare and raise issue: https://github.com/CatalogueOfLife/backend/issues/743
        }
      }
    }
    return opt.map(NeoName::new);
  }
  
  private Classification interpretClassification(VerbatimRecord v) {
    Classification cl = new Classification();
    for (ColdpTerm term : ColdpTerm.DENORMALIZED_RANKS) {
      cl.setByTerm(term, v.get(term));
    }
    return cl;
  }
  
  private void setReference(Referenced obj, VerbatimRecord v) {
    super.setReference(v, ColdpTerm.referenceID, obj::setReferenceId);
  }

}
