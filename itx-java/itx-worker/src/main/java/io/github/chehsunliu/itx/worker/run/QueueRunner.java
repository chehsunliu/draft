package io.github.chehsunliu.itx.worker.run;

import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import sun.misc.Signal;

@Slf4j
public final class QueueRunner {
  private QueueRunner() {}

  /**
   * Runs queue listeners in parallel, dispatching every message to {@code handler}. Returns when
   * SIGTERM/SIGINT is received and all listeners have drained.
   */
  public static void runQueueLoops(
      List<MessageQueue> queues, MessageHandler handler, boolean registerSignals) {
    AtomicBoolean cancel = new AtomicBoolean(false);

    if (registerSignals) {
      Signal.handle(new Signal("INT"), s -> cancel.set(true));
      Signal.handle(new Signal("TERM"), s -> cancel.set(true));
    }

    ExecutorService listeners =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("queue-listener-", 0).factory());
    List<Future<?>> futures = new ArrayList<>(queues.size());
    for (MessageQueue q : queues) {
      futures.add(
          listeners.submit(
              () -> {
                try {
                  q.receive(handler, cancel);
                  log.info("queue listener returned cleanly");
                } catch (Throwable t) {
                  log.error("queue listener errored", t);
                }
              }));
    }
    for (Future<?> f : futures) {
      try {
        f.get();
      } catch (Exception ignored) {
        // already logged in the listener
      }
    }
    listeners.shutdown();
    try {
      if (!listeners.awaitTermination(5, TimeUnit.SECONDS)) listeners.shutdownNow();
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      listeners.shutdownNow();
    }
  }
}
