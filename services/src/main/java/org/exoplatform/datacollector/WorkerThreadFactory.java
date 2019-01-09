package org.exoplatform.datacollector;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker thread factory adapted from {@link Executors#DefaultThreadFactory}.
 */
public class WorkerThreadFactory implements ThreadFactory {

  /** The group. */
  final ThreadGroup   group;

  /** The thread number. */
  final AtomicInteger threadNumber = new AtomicInteger(1);

  /** The name prefix. */
  final String        namePrefix;

  /**
   * Instantiates a new command thread factory.
   *
   * @param namePrefix the name prefix
   */
  WorkerThreadFactory(String namePrefix) {
    SecurityManager s = System.getSecurityManager();
    this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    this.namePrefix = namePrefix;
  }

  public Thread newThread(Runnable r) {
    Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0) {

      /**
       * {@inheritDoc}
       */
      @Override
      protected void finalize() throws Throwable {
        super.finalize();
        threadNumber.decrementAndGet();
      }

    };
    if (t.isDaemon()) {
      t.setDaemon(false);
    }
    if (t.getPriority() != Thread.NORM_PRIORITY) {
      t.setPriority(Thread.NORM_PRIORITY);
    }
    return t;
  }
}