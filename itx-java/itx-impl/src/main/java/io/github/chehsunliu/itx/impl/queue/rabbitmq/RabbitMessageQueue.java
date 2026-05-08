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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RabbitMessageQueue implements MessageQueue {

  private final Connection connection;
  private final String queueName;
  private final String consumerTag = "itx-" + UUID.randomUUID();
  private final int maxConcurrency;

  /**
   * Per RabbitMQ best practice the publish channel is separate from the consume channel — channels
   * aren't safe for mixed concurrent reads and writes.
   */
  private volatile Channel publishChannel;

  private volatile Channel consumeChannel;

  public RabbitMessageQueue(Connection connection, String queueName, int maxConcurrency) {
    this.connection = connection;
    this.queueName = queueName;
    this.maxConcurrency = maxConcurrency;
  }

  @Override
  public synchronized void publish(String body) {
    try {
      if (publishChannel == null) {
        publishChannel = connection.createChannel();
      }
      publishChannel.basicPublish(
          "", // default exchange — routing_key acts as queue name
          queueName,
          MessageProperties.PERSISTENT_TEXT_PLAIN,
          body.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new QueueException(e);
    }
  }

  @Override
  public void receive(MessageHandler handler, AtomicBoolean cancel) {
    Channel channel;
    try {
      synchronized (this) {
        if (consumeChannel == null) {
          consumeChannel = connection.createChannel();
          // Match prefetch to max_concurrency so the broker doesn't dispatch faster than
          // we can hold semaphore permits. u16 cap because basic.qos prefetch is u16.
          int prefetch = Math.min(maxConcurrency, 0xFFFF);
          consumeChannel.basicQos(prefetch);
        }
        channel = consumeChannel;
      }
    } catch (IOException e) {
      throw new QueueException(e);
    }

    Semaphore inflight = new Semaphore(maxConcurrency);
    ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    try {
      channel.basicConsume(
          queueName,
          false,
          consumerTag,
          new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                String tag, Envelope envelope, AMQP.BasicProperties props, byte[] body) {
              if (cancel.get()) {
                try {
                  channel.basicReject(envelope.getDeliveryTag(), true);
                } catch (IOException e) {
                  log.warn("requeue on cancel failed", e);
                }
                return;
              }
              inflight.acquireUninterruptibly();
              workers.execute(
                  () -> {
                    try {
                      String text = new String(body, StandardCharsets.UTF_8);
                      try {
                        handler.handle(text);
                      } catch (Exception e) {
                        log.warn("handler failed; rejecting to DLQ", e);
                        try {
                          channel.basicReject(envelope.getDeliveryTag(), false);
                        } catch (IOException io) {
                          log.error("failed to reject after handler failure", io);
                        }
                        return;
                      }
                      try {
                        channel.basicAck(envelope.getDeliveryTag(), false);
                      } catch (IOException io) {
                        log.error("failed to ack message after success", io);
                      }
                    } finally {
                      inflight.release();
                    }
                  });
            }
          });

      while (!cancel.get()) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      try {
        channel.basicCancel(consumerTag);
      } catch (IOException e) {
        log.warn("basicCancel failed", e);
      }
    } catch (IOException e) {
      throw new QueueException(e);
    } finally {
      log.info("draining in-flight handlers for queue {}", queueName);
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
