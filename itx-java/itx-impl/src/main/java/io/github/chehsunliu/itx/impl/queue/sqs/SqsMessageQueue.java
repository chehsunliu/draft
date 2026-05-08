package io.github.chehsunliu.itx.impl.queue.sqs;

import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Slf4j
@RequiredArgsConstructor
public class SqsMessageQueue implements MessageQueue {

  private final SqsClient client;
  private final String queueUrl;
  private final int maxConcurrency;

  @Override
  public void publish(String body) {
    try {
      client.sendMessage(b -> b.queueUrl(queueUrl).messageBody(body));
    } catch (RuntimeException e) {
      throw new QueueException(e);
    }
  }

  @Override
  public void receive(MessageHandler handler, AtomicBoolean cancel) {
    int batch = Math.min(10, Math.max(1, maxConcurrency));
    Semaphore inflight = new Semaphore(maxConcurrency);
    ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    try {
      while (!cancel.get()) {
        var resp =
            client.receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(batch)
                    .waitTimeSeconds(20)
                    .build());

        for (Message msg : resp.messages()) {
          if (cancel.get()) break;

          inflight.acquireUninterruptibly();
          workers.execute(
              () -> {
                try {
                  try {
                    handler.handle(msg.body());
                    client.deleteMessage(
                        b -> b.queueUrl(queueUrl).receiptHandle(msg.receiptHandle()));
                  } catch (Exception e) {
                    // Failure: skip delete. The message becomes visible again after the
                    // visibility timeout, eventually landing in the DLQ once it exceeds
                    // maxReceiveCount.
                    log.warn("handler failed; leaving message for retry/DLQ", e);
                  }
                } finally {
                  inflight.release();
                }
              });
        }
      }
    } catch (RuntimeException e) {
      throw new QueueException(e);
    } finally {
      log.info("draining in-flight handlers for queue {}", queueUrl);
      workers.shutdown();
      try {
        if (!workers.awaitTermination(30, TimeUnit.SECONDS)) {
          workers.shutdownNow();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        workers.shutdownNow();
      }
    }
  }
}
