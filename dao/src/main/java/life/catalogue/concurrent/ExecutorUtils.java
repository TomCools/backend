package life.catalogue.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ExecutorUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ExecutorUtils.class);
  
  // milliseconds to wait during shutdown before forcing a shutdown
  public static final int MILLIS_TO_DIE = 12000;
  
  /**
   * Shutdown executor and wait until all jobs are done no matter how long it takes.
   * (actually waits for one month at most).
   */
  public static void shutdown(ExecutorService exec) {
    shutdown(exec, 31, TimeUnit.DAYS);
  }
  
  public static void shutdown(ExecutorService exec, int timeout, TimeUnit unit) {
    try {
      LOG.info("attempt to shutdown executor within {} {}", timeout, unit);
      exec.shutdown();
      if (exec.awaitTermination(timeout, unit)) {
        LOG.info("shutdown succeeded orderly");
      } else {
        forceShutdown(exec);
      }
      
    } catch (InterruptedException e) {
      LOG.info("executor shutdown interrupted, force immediate shutdown");
      forceShutdown(exec);
    }
  }
  
  private static void forceShutdown(ExecutorService exec) {
    int count = exec.shutdownNow().size();
    LOG.warn("forced shutdown, discarding {} queued tasks", count);
  }
}
