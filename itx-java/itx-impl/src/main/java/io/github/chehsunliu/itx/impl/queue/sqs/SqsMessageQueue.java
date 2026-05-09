package io.github.chehsunliu.itx.impl.queue.sqs;

import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Slf4j
@RequiredArgsConstructor
public final class SqsMessageQueue implements MessageQueue {
  private final SqsClient client;
  private final String queueUrl;
  private final int maxConcurrency;

  @Override
  public void publish(String body) {
    try {
      client.sendMessage(b -> b.queueUrl(queueUrl).messageBody(body));
    } catch (RuntimeException e) {
      throw new QueueException(e.getMessage() == null ? "publish failed" : e.getMessage(), e);
    }
  }

  @Override
  public void receive(MessageHandler handler, AtomicBoolean cancel) {
    int batch = Math.min(10, Math.max(1, maxConcurrency));
    Semaphore permits = new Semaphore(maxConcurrency);
    ExecutorService workers =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("sqs-worker-", 0).factory());
    List<Future<?>> jobs = new ArrayList<>();

    try {
      while (!cancel.get()) {
        ReceiveMessageResponse resp;
        try {
          resp =
              client.receiveMessage(
                  ReceiveMessageRequest.builder()
                      .queueUrl(queueUrl)
                      .maxNumberOfMessages(batch)
                      .waitTimeSeconds(20)
                      .build());
        } catch (RuntimeException e) {
          throw new QueueException(e.getMessage() == null ? "receive failed" : e.getMessage(), e);
        }
        for (Message msg : resp.messages()) {
          if (cancel.get()) break;
          try {
            permits.acquire();
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
          }
          jobs.add(
              workers.submit(
                  () -> {
                    try {
                      handler.handle(msg.body());
                      client.deleteMessage(
                          b -> b.queueUrl(queueUrl).receiptHandle(msg.receiptHandle()));
                    } catch (Throwable t) {
                      // Failure: skip delete. The message becomes visible again after the
                      // visibility timeout, eventually landing in the DLQ once it exceeds
                      // maxReceiveCount.
                      log.warn("handler failed; leaving message for retry/DLQ", t);
                    } finally {
                      permits.release();
                    }
                  }));
        }
      }
      log.info("draining in-flight handlers for queue {}", queueUrl);
      for (Future<?> f : jobs) {
        try {
          f.get();
        } catch (Exception ignored) {
          // already logged in the worker
        }
      }
    } finally {
      workers.shutdown();
      try {
        if (!workers.awaitTermination(5, TimeUnit.SECONDS)) workers.shutdownNow();
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        workers.shutdownNow();
      }
    }
  }
}
