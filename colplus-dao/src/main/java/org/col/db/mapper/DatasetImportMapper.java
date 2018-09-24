package org.col.db.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.col.api.model.DatasetImport;
import org.col.api.model.Page;
import org.col.api.vocab.*;
import org.col.db.type2.IntCount;
import org.col.db.type2.StringCount;
import org.gbif.dwc.terms.Term;
import org.gbif.nameparser.api.NameType;
import org.gbif.nameparser.api.Rank;

/**
 * The MyBatis mapper interface for DatasetImport.
 */
public interface DatasetImportMapper {

  /**
   * Count all imports by their state
   */
  int count(@Param("states") Collection<ImportState> states);

  /**
   * List all imports optionally filtered by their state
   */
  List<DatasetImport> list(@Param("states") @Nullable Collection<ImportState> states, @Param("page") Page page);

  /**
   * List current and historical imports for a dataset ordered by attempt from last to historical.
   * Optionally filtered and limited, e.g. by one to get the last only.
   */
  List<DatasetImport> listByDataset(@Param("key") int datasetKey, @Param("state") @Nullable ImportState state, @Param("limit") int limit);

  void create(@Param("di") DatasetImport datasetImport);

  void update(@Param("di") DatasetImport datasetImport);

  Integer countDistribution(@Param("key") int datasetKey);
  Integer countName(@Param("key") int datasetKey);
  Integer countReference(@Param("key") int datasetKey);
  Integer countTaxon(@Param("key") int datasetKey);
  Integer countVerbatim(@Param("key") int datasetKey);
  Integer countVernacular(@Param("key") int datasetKey);

  List<IntCount> countDistributionsByGazetteer(@Param("key") int datasetKey);
  List<IntCount> countIssues(@Param("key") int datasetKey);
  List<IntCount> countNameRelationsByType(@Param("key") int datasetKey);
  List<IntCount> countNamesByOrigin(@Param("key") int datasetKey);
  List<StringCount> countNamesByRank(@Param("key") int datasetKey);
  List<IntCount> countNamesByStatus(@Param("key") int datasetKey);
  List<IntCount> countNamesByType(@Param("key") int datasetKey);
  List<IntCount> countUsagesByStatus(@Param("key") int datasetKey);
  List<StringCount> countVerbatimByType(@Param("key") int datasetKey);
  List<StringCount> countVernacularsByLanguage(@Param("key") int datasetKey);


}
