package life.catalogue.db.mapper;

import life.catalogue.api.vocab.DatasetOrigin;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Mapper with default methods to manage the lifetime of dataset based partitions
 * and all their need sequences & indices.
 *
 * We deal with dataset partitioning depending on the immutable origin property of the dataset:
 *
 * EXTERNAL: HASH based partitioning on the datasetKey
 * PROJECT: a dedicated partition which provides fast read/write speeds
 * RELEASE: a dedicated partition which provides fast read/write speeds and allows for quick deletions of entire releases when they get old
 *
 * This assumes that the total number of managed and released datasets is well below 1000.
 * If we ever exceed these numbers we should relocate partitions.
 *
 * For COL itself it is important to have speedy releases and quick responses when managing the project,
 * so these should always live on their own partition.
 */
public interface DatasetPartitionMapper {
  Logger LOG = LoggerFactory.getLogger(DatasetPartitionMapper.class);

  List<String> IDMAP_TABLES = Lists.newArrayList(
    "idmap_name",
    "idmap_name_usage"
  );

  // order is important !!!
  List<String> PARTITIONED_TABLES = Lists.newArrayList(
      "verbatim",
      "reference",
      "name",
      "name_rel",
      "type_material",
      "name_usage",
      "taxon_concept_rel",
      "species_interaction",
      "distribution",
      "media",
      "treatment",
      "vernacular_name"
  );

  List<String> NON_PARTITIONED_TABLES = Lists.newArrayList(
    "name_match",
    "estimate"

  );

  List<String> PROJECT_TABLES = Lists.newArrayList(
    "verbatim_source",
    "verbatim_source_secondary"
  );
  
  List<String> SERIAL_TABLES = Lists.newArrayList(
      "verbatim",
      "name_rel",
      "taxon_concept_rel",
      "species_interaction",
      "distribution",
      "media",
      "vernacular_name"
  );

  List<String> PROJECT_SERIAL_TABLES = Lists.newArrayList(
    "sector",
    "decision"
  );

  /**
   * Creates the default partition and its hashed subpartitions
   * @param number of subpartitions
   * @param createDefaultPartition if true also creates the single default parent partition
   */
  default void createDefaultPartitions(int number, boolean createDefaultPartition) {
    if (createDefaultPartition) {
      PARTITIONED_TABLES.forEach(this::createDefaultPartition);
    }
    for (int i=0; i<number; i++) {
      int remainder = i;
      String suffix = "mod"+remainder;
      PARTITIONED_TABLES.forEach(t -> createDefaultSubPartition(t, number, remainder));
      // create triggers
      attachTriggers(suffix);
    }
  }

  void createDefaultPartition(@Param("table") String table);

  void createDefaultSubPartition(@Param("table") String table, @Param("modulus") int modulus, @Param("remainder") int remainder);

  /**
   * Creates a new dataset partition for all data tables if not already existing.
   * The tables are not attached to the main partitioned table yet, see attach().
   * Indices are not created yet which should happen after the data is inserted.
   * Tables with integer id columns will have their own sequence.
   */
  default void create(int key, DatasetOrigin origin) {
    // create dataset specific sequences regardless which origin
    SERIAL_TABLES.forEach(t -> createIdSequence(t, key));
    // estimates can exist also in non managed datasets, so we need to have an id sequence for them in all datasets
    // but they are not a partitioned table, treat them special
    createIdSequence("estimate", key);
    // things specific to managed and released datasets only
    if (origin.isManagedOrRelease()) {
      // sequences needed to generate keys in managed datasets
      if (origin == DatasetOrigin.PROJECT) {
        createManagedSequences(key);
      }
      PARTITIONED_TABLES.forEach(t -> {
        createTable(t, key);
        createDatasetKeyCheck(t, key);
      });
      PROJECT_TABLES.forEach(t -> createTable(t, key));
    }
  }
  
  void createTable(@Param("table") String table, @Param("key") int key);

  void createIdMapTable(@Param("table") String table, @Param("key") int key);

  void createDatasetKeyCheck(@Param("table") String table, @Param("key") int key);

  /**
   * Creates a new id sequence and uses it as the default value (serial) for the given tables id column.
   * @param table
   * @param key
   */
  void createSerial(@Param("table") String table, @Param("key") int key);

  /**
   * Creates a new standalone id sequence named after the table and dataset key
   * @param table
   * @param key
   */
  void createIdSequence(@Param("table") String table, @Param("key") int key);

  /**
   * Updates the sequences for a given datasetKey to the current max of existing keys.
   * @param key datasetKey
   */
  default void updateIdSequences(int key) {
    SERIAL_TABLES.forEach(t -> updateIdSequence(t, key));
  }
  
  void updateIdSequence(@Param("table") String table, @Param("key") int key);

  void deleteIdSequence(@Param("table") String table, @Param("key") int key);

  default void createManagedSequences(@Param("key") int key) {
    PROJECT_SERIAL_TABLES.forEach(t -> createIdSequence(t, key));
  }

  /**
   * Updates the managed sequences for a given datasetKey to the current max of existing keys.
   * @param key datasetKey
   */
  default void updateManagedSequences(int key) {
    PROJECT_SERIAL_TABLES.forEach(t -> updateIdSequence(t, key));
  }

  default void deleteManagedSequences(@Param("key") int key) {
    PROJECT_SERIAL_TABLES.forEach(t -> deleteIdSequence(t, key));
  }

  /**
   * Deletes an entire partition if its dataset specific or deletes all data from shared partitions, e.g. for external datasets.
   * Leaves the dataset record itself untouched.
   *
   * @param key
   */
  default void delete(int key, DatasetOrigin origin) {
    LOG.info("Delete non partitioned data for {} dataset {}", origin, key);
    Lists.reverse(NON_PARTITIONED_TABLES).forEach(t -> deleteData(t, key));
    deleteUsageCounter(key);

    if (origin.isManagedOrRelease()) {
      LOG.info("Drop partition tables for {} dataset {}", origin, key);
      PROJECT_TABLES.forEach(t -> dropTable(t, key));
      Lists.reverse(PARTITIONED_TABLES).forEach(t -> dropTable(t, key));
      IDMAP_TABLES.forEach(t -> dropTable(t, key));
    } else {
      LOG.info("Delete data for {} dataset {}", origin, key);
      Lists.reverse(PARTITIONED_TABLES).forEach(t -> deleteData(t, key));
      SERIAL_TABLES.forEach(t -> deleteIdSequence(t, key));
    }
  }

  /**
   * Deletes all data from a table for the given datasetKey.
   * @param key datasetKey
   */
  void deleteData(@Param("table") String table, @Param("key") int key);

  void dropTable(@Param("table") String table, @Param("key") int key);

  void deleteUsageCounter(@Param("key") int key);
  
  /**
   * Attaches all dataset specific partition tables to their main table
   * so they become visible.
   *
   * Warning! This requires an AccessExclusiveLock on the main tables
   * which can lead to deadlocks, see https://github.com/Sp2000/colplus-backend/issues/387
   *
   * Make sure constraints are in place on both the default partition and the table being attached so that no table scans are
   * needed which would lock the main table for a long time. Otherwise attaching doesncan be fast!
   *
   * @param key
   */
  default void attach(int key, DatasetOrigin origin) {
    if (origin.isManagedOrRelease()) {
      // attach, this also creates indices, pks and fks
      PARTITIONED_TABLES.forEach(t -> attachTable(t, key));
      // create triggers
      final String suffix = String.valueOf(key);
      attachTriggers(suffix);
      // things specific to managed datasets only
      PROJECT_TABLES.forEach(t -> attachTable(t, key));
    }
  }
  
  void attachTable(@Param("table") String table, @Param("key") int key);

  void detachTable(@Param("table") String table, @Param("key") int key);

  /**
   * Attaches all required triggers for a given partition suffix.
   * Currently these are 1 trigger on the name and 2 triggers on the name usage partition.
   * Make sure to call this AFTER the partition table is attached.
   * @param suffix partition suffix, e.g. datasetkey for external datasets
   */
  void attachTriggers(@Param("key") String suffix);

  /**
   * Updates the name usage counter record with the current count.
   * Make sure to call this AFTER the partition table is attached
   * @param key datasetkey
   */
  int updateUsageCounter(@Param("key") int key);

  /**
   * Checks whether the partition for the given datasetKey exists already.
   * @param key datasetKey
   * @return true if partition tables exist
   */
  default boolean exists(@Param("key") int key, DatasetOrigin origin) {
    if (origin.isManagedOrRelease()) {
      return existsDatasetSpecific(key);
    } else {
      return true;
    }
  }

  /**
   * @return if partitions exist specific only for the given dataset, i.e. managed or released.
   */
  boolean existsDatasetSpecific(@Param("key") int key);

  /**
   * Lists all dataset keys for which there is an existing name partition table
   */
  List<Integer> existingPartitions();

  /**
   * Return the list of columns for a given table igoring "doc" columns
   */
  List<String> columns(@Param("t") String table);

  /**
   * Removes and recreates dataset key check contraints on all default tables
   * with a check that all keys are either below or beyond a given value.
   */
  default void updateDatasetKeyChecks(int below, int beyond) {
    PARTITIONED_TABLES.forEach(this::dropDatasetKeyCheck);
    PARTITIONED_TABLES.forEach(t -> addDatasetKeyCheck(t, below, beyond));
  }

  void dropDatasetKeyCheck(@Param("table") String table);

  void addDatasetKeyCheck(@Param("table") String table, @Param("below") int below, @Param("beyond") int beyond);

}
