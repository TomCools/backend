package life.catalogue.db.mapper;

import life.catalogue.api.model.DSID;
import life.catalogue.api.model.IssueContainer;
import life.catalogue.api.model.VerbatimSource;
import life.catalogue.api.vocab.Issue;
import life.catalogue.db.*;

import java.util.Set;

import org.apache.ibatis.annotations.Param;

/**
 *
 */
public interface VerbatimSourceMapper extends Create<VerbatimSource>, CopyDataset, DatasetProcessable<VerbatimSource>,
  SectorProcessable<VerbatimSource>, TempNameUsageRelated {

  VerbatimSource get(@Param("key") DSID<String> key);
  
  IssueContainer getIssues(@Param("key") DSID<String> key);

  void update(@Param("key") DSID<String> key, @Param("issues") Set<Issue> issues);

  void delete(@Param("key") DSID<String> key);

}

