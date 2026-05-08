package io.github.chehsunliu.itx.worker.run;

import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

/**
 * Runs queue listeners that consume from one or more queues, dispatching every message to a shared
 * {@link MessageHandler}. Hooks into Spring's lifecycle so SIGINT/SIGTERM (which trigger the
 * application context shutdown) cleanly cancels each listener and waits up to 30 seconds for
 * in-flight handlers to drain.
 */
@Slf4j
public class QueueRunner implements SmartLifecycle {

  private final List<MessageQueue> queues;
  private final MessageHandler handler;
  private final AtomicBoolean cancel = new AtomicBoolean(false);
  private ExecutorService listeners;
  private volatile boolean running;

  public QueueRunner(List<MessageQueue> queues, MessageHandler handler) {
    this.queues = queues;
    this.handler = handler;
  }

  @Override
  public void start() {
    if (running) return;
    listeners = Executors.newCachedThreadPool();
    for (MessageQueue queue : queues) {
      listeners.submit(
          () -> {
            try {
              queue.receive(handler, cancel);
              log.info("queue listener returned cleanly");
            } catch (Exception e) {
              log.error("queue listener errored", e);
            }
          });
    }
    running = true;
  }

  @Override
  public void stop() {
    if (!running) return;
    log.info("shutdown requested; cancelling listeners");
    cancel.set(true);
    listeners.shutdown();
    try {
      if (!listeners.awaitTermination(30, TimeUnit.SECONDS)) {
        log.warn("some listeners did not finish within 30s");
        listeners.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      listeners.shutdownNow();
    }
    running = false;
    log.info("worker shutdown complete");
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }
}
