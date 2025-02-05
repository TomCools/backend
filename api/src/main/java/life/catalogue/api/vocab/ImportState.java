package life.catalogue.api.vocab;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Warning! If ordinals are changed please change also DatasetImportMapper.xml
 * which has a hardcoded number!
 */
public enum ImportState {
  
  /**
   * Queued.
   */
  WAITING(false),

  /**
   * Any steps need to prepare or initialize a job.
   */
  PREPARING(true),

  /**
   * Downloading the latest source data, the first step of a running import.
   */
  DOWNLOADING(true),
  
  /**
   * Normalization of the dataset without touching the previous data in Postgres.
   */
  PROCESSING(true),

  /**
   * Deleting data in Postgres
   */
  DELETING(true),

  /**
   * Inserting data into Postgres, starts by wiping any previous edition.
   */
  INSERTING(true),

  /**
   * Rematching ids, decisions, sectors, estimates or parent taxa
   */
  MATCHING(true),

  /**
   * Indexing data into the Elastic Search index.
   */
  INDEXING(true),

  /**
   * Building metrics, preparing indices or doing other analytics.
   */
  ANALYZING(true),

  /**
   * Archive dataset metadata or metrics
   */
  ARCHIVING(true),

  /**
   * Exporting data to archives.
   */
  EXPORTING(true),

  /**
   * Sources have not been changed since last import. Imported stopped.
   */
  UNCHANGED,
  
  /**
   * Successfully completed the import/release/duplication job.
   */
  FINISHED,

  /**
   * Manually aborted import, e.g. system was shut down.
   */
  CANCELED,
  
  /**
   * Import failed due to errors.
   */
  FAILED;
  
  ImportState() {
    this.running = false;
  }

  ImportState(boolean running) {
    this.running = running;
  }
  
  private final boolean running;
  
  public boolean isRunning() {
    return running;
  }
  
  public boolean isQueued() {
    return this == WAITING;
  }

  /**
   * @return true if its a final state after a job has ended.
   */
  public boolean isFinished() {
    return !isQueued() && !isRunning();
  }

  public static List<ImportState> runningStates() {
    return Arrays.stream(values())
        .filter(ImportState::isRunning)
        .collect(Collectors.toList());
  }

  public static List<ImportState> runningAndWaitingStates() {
    return Arrays.stream(values())
                 .filter(s -> s.isRunning() || s == WAITING)
                 .collect(Collectors.toList());
  }

  public static List<ImportState> finishedStates() {
    return Arrays.stream(values())
        .filter(ImportState::isFinished)
        .collect(Collectors.toList());
  }
}
