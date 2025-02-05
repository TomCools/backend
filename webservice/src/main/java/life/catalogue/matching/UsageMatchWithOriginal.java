package life.catalogue.matching;

import life.catalogue.api.model.IssueContainer;
import life.catalogue.api.model.SimpleName;
import life.catalogue.api.model.SimpleNameClassified;
import life.catalogue.matching.UsageMatch;

public class UsageMatchWithOriginal extends UsageMatch {
  public final SimpleNameClassified<SimpleName> original;
  public final IssueContainer issues;

  public UsageMatchWithOriginal(UsageMatch match, IssueContainer issues, SimpleNameClassified<SimpleName> original) {
    super(match.datasetKey, match.usage, match.sourceDatasetKey, match.type, match.ignore, match.doubtfulUsage, match.alternatives);
    this.original = original;
    this.issues = issues;
  }
}
