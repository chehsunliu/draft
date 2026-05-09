package io.github.chehsunliu.itx.impl.queue.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class RabbitMessageQueue implements MessageQueue {
  private final Connection connection;
  private final String queueName;
  private final int maxConcurrency;
  private final String consumerTag = "itx-" + UUID.randomUUID();

  // Per RabbitMQ best practice the publish channel is separate from the consume channel —
  // channels aren't safe for mixed concurrent reads and writes.
  private volatile Channel publishChannel;
  private volatile Channel consumeChannel;
  private final Object publishLock = new Object();

  @Override
  public void publish(String body) {
    try {
      Channel ch;
      synchronized (publishLock) {
        if (publishChannel == null) publishChannel = connection.createChannel();
        ch = publishChannel;
      }
      ch.basicPublish(
          "", // default exchange — routing_key acts as queue name
          queueName,
          MessageProperties.PERSISTENT_TEXT_PLAIN,
          body.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new QueueException(e.getMessage() == null ? "publish failed" : e.getMessage(), e);
    }
  }

  @Override
  public void receive(MessageHandler handler, AtomicBoolean cancel) {
    Channel channel;
    try {
      if (consumeChannel == null) {
        Channel ch = connection.createChannel();
        int prefetch = Math.min(maxConcurrency, 0xFFFF);
        ch.basicQos(prefetch);
        consumeChannel = ch;
      }
      channel = consumeChannel;
    } catch (IOException e) {
      throw new QueueException(e.getMessage() == null ? "channel open failed" : e.getMessage(), e);
    }

    Semaphore permits = new Semaphore(maxConcurrency);
    ExecutorService workers =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("rmq-worker-", 0).factory());
    List<Future<?>> jobs = new ArrayList<>();

    try {
      channel.basicConsume(
          queueName,
          /* autoAck= */ false,
          consumerTag,
          new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                String tag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
              if (cancel.get()) {
                try {
                  channel.basicReject(envelope.getDeliveryTag(), true);
                } catch (IOException e) {
                  log.warn("requeue on cancel failed", e);
                }
                return;
              }
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
                          String text = new String(body, StandardCharsets.UTF_8);
                          try {
                            handler.handle(text);
                            try {
                              channel.basicAck(envelope.getDeliveryTag(), false);
                            } catch (IOException io) {
                              log.error("failed to ack message after success", io);
                            }
                          } catch (Throwable t) {
                            log.warn("handler failed; rejecting to DLQ", t);
                            try {
                              channel.basicReject(envelope.getDeliveryTag(), false);
                            } catch (IOException io) {
                              log.error("failed to reject after handler failure", io);
                            }
                          }
                        } finally {
                          permits.release();
                        }
                      }));
            }
          });

      while (!cancel.get()) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      try {
        channel.basicCancel(consumerTag);
      } catch (IOException e) {
        log.warn("basicCancel failed", e);
      }

      log.info("draining in-flight handlers for queue {}", queueName);
      for (Future<?> f : jobs) {
        try {
          f.get();
        } catch (Exception ignored) {
          // already logged in the worker
        }
      }
    } catch (IOException e) {
      throw new QueueException(e.getMessage() == null ? "receive failed" : e.getMessage(), e);
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
