package life.catalogue.importer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import life.catalogue.api.model.IssueContainer;
import life.catalogue.api.model.Name;
import life.catalogue.api.model.VerbatimRecord;
import life.catalogue.api.vocab.Issue;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 *
 */
public class NameValidator {
  private static final Logger LOG = LoggerFactory.getLogger(NameValidator.class);
  private static final Pattern WHITE = Pattern.compile("\\s");
  @VisibleForTesting
  // ë is exceptionally allowed in botanical code. See Article 60.6
  // The diaeresis, indicating that a vowel is to be pronounced separately from the preceding vowel (as in Cephaëlis, Isoëtes), is a phonetic device that is not considered to alter the spelling; as such, its use is optional
  static final Pattern NON_LETTER = Pattern.compile("[^a-z-ë]", Pattern.CASE_INSENSITIVE);
  static final CharMatcher OPEN_BRACKETS = CharMatcher.anyOf("[({");
  static final CharMatcher CLOSE_BRACKETS = CharMatcher.anyOf("])}");

  
  static class LazyVerbatimRecord implements IssueContainer {
    private VerbatimRecord container;
    private final Supplier<VerbatimRecord> supplier;
    private int startSize;
  
    LazyVerbatimRecord(Supplier<VerbatimRecord> supplier) {
      this.supplier = supplier;
    }
    
    private void load() {
      if (container == null) {
        container = supplier.get();
        startSize = container.getIssues().size();
      }
    }
  
    @Override
    public Set<Issue> getIssues() {
      load();
      return container.getIssues();
    }
  
    @Override
    public void setIssues(Set<Issue> issues) {
      load();
      container.setIssues(issues);
    }
  
    @Override
    public void addIssue(Issue issue) {
      load();
      container.addIssue(issue);
    }
  
    @Override
    public boolean removeIssue(Issue issue) {
      return container.removeIssue(issue);
    }
  
    @Override
    public boolean hasIssue(Issue issue) {
      load();
      return container.hasIssue(issue);
    }
  
    public boolean hasChanged() {
      return container != null && container.getIssues().size() > startSize;
    }
  }
  
  public static VerbatimRecord flagIssues(Name n, VerbatimRecord v) {
    return flagIssues(n, new Supplier<VerbatimRecord>() {
      @Override
      public VerbatimRecord get() {
        return v;
      }
    });
  }
  
  /**
   * Validates consistency of name properties adding issues to the name if found.
   * This method checks if the given rank matches
   * populated propLabel and available propLabel make sense together.
   * @return a non null VerbatimRecord if any issue have been added
   */
  public static VerbatimRecord flagIssues(Name n, Supplier<VerbatimRecord> issueSupplier) {
    final LazyVerbatimRecord v = new LazyVerbatimRecord(issueSupplier);
    // only check for type scientific which is parsable
    if (n.getType() == NameType.SCIENTIFIC && n.isParsed()) {
      flagParsedIssues(n, v);
    } else {
      // flag issues on the full name for unparsed names
      if (hasUnmatchedBrackets(n.getScientificName())) {
        v.addIssue(Issue.UNMATCHED_NAME_BRACKETS);
      }
    }
    return v.hasChanged() ? v.container : null;
  }
  
  public static boolean hasUnmatchedBrackets(String x) {
    return !Strings.isNullOrEmpty(x) && OPEN_BRACKETS.countIn(x) != CLOSE_BRACKETS.countIn(x);
  }
  
  private static void flagParsedIssues(Name n, IssueContainer issues) {
    final Rank rank = n.getRank();
    if (n.getUninomial() != null && (n.getGenus() != null || n.getInfragenericEpithet() != null
        || n.getSpecificEpithet() != null || n.getInfraspecificEpithet() != null)) {
      LOG.info("Uninomial with further epithets in name {}", n.toStringComplete());
      issues.addIssue(Issue.INCONSISTENT_NAME);
      
    } else if (n.getGenus() == null && (n.getSpecificEpithet() != null || n.getInfragenericEpithet() != null)) {
      LOG.info("Missing genus in name {}", n.toStringComplete());
      issues.addIssue(Issue.INCONSISTENT_NAME);
      
    } else if (n.getSpecificEpithet() == null && n.getInfraspecificEpithet() != null) {
      LOG.info("Missing specific epithet in infraspecific name {}", n.toStringComplete());
      issues.addIssue(Issue.INCONSISTENT_NAME);
    }
    
    // look for truncated authorship
    if (hasUnmatchedBrackets(n.getAuthorship())) {
      issues.addIssue(Issue.UNMATCHED_NAME_BRACKETS);
    }
    
    // verify epithets
    for (String epithet : n.nameParts()) {
      // no whitespace
      if (WHITE.matcher(epithet).find()) {
        LOG.info("Name part contains whitespace {}", n.toStringComplete());
        issues.addIssue(Issue.UNUSUAL_NAME_CHARACTERS);
      }
      // non ascii chars
      if (NON_LETTER.matcher(epithet).find()) {
        LOG.info("Name part contains non ASCII letters {}", n.toStringComplete());
        issues.addIssue(Issue.UNUSUAL_NAME_CHARACTERS);
      }
    }
    
    // verify ranks
    if (rank != null && rank.notOtherOrUnranked()) {
      if (rank.isGenusOrSuprageneric()) {
        if (n.getGenus() != null || n.getUninomial() == null) {
          LOG.info("Missing genus or uninomial for {}", n.toStringComplete());
          issues.addIssue(Issue.INCONSISTENT_NAME);
        }
        
      } else if (rank.isInfrageneric() && rank.isSupraspecific()) {
        if (n.getInfragenericEpithet() == null) {
          LOG.info("Missing infrageneric epithet for {}", n.toStringComplete());
          issues.addIssue(Issue.INCONSISTENT_NAME);
        }
        
        if (n.getSpecificEpithet() != null || n.getInfraspecificEpithet() != null) {
          LOG.info("Species or infraspecific epithet for {}", n.toStringComplete());
          issues.addIssue(Issue.INCONSISTENT_NAME);
        }
        
      } else if (rank.isSpeciesOrBelow()) {
        if (n.getSpecificEpithet() == null) {
          LOG.info("Missing specific epithet for {}", n.toStringComplete());
          issues.addIssue(Issue.INCONSISTENT_NAME);
        }
        
        if (!rank.isInfraspecific() && n.getInfraspecificEpithet() != null) {
          LOG.info("Infraspecific epithet found for {}", n.toStringComplete());
          issues.addIssue(Issue.INCONSISTENT_NAME);
        }
      }
      
      if (rank.isInfraspecific()) {
        if (n.getInfraspecificEpithet() == null) {
          LOG.info("Missing infraspecific epithet for {}", n.toStringComplete());
          issues.addIssue(Issue.INCONSISTENT_NAME);
        }
      }
    }
  }
}
