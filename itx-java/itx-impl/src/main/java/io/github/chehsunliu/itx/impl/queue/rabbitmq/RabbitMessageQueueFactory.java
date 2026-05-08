package io.github.chehsunliu.itx.impl.queue.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMessageQueueFactory implements MessageQueueFactory {

  private record Config(
      String host,
      int port,
      String user,
      String password,
      int maxConcurrency,
      String controlStandardQueue,
      String controlPremiumQueue,
      String computeStandardQueue,
      String computePremiumQueue) {}

  private final Connection connection;
  private final Config config;

  public RabbitMessageQueueFactory() {
    Config cfg =
        new Config(
            requireEnv("ITX_RABBITMQ_HOST"),
            Integer.parseInt(requireEnv("ITX_RABBITMQ_PORT")),
            requireEnv("ITX_RABBITMQ_USER"),
            requireEnv("ITX_RABBITMQ_PASSWORD"),
            envInt("ITX_RABBITMQ_MAX_CONCURRENCY", 100),
            requireEnv("ITX_RABBITMQ_CONTROL_STANDARD_QUEUE"),
            requireEnv("ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE"),
            requireEnv("ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE"),
            requireEnv("ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE"));

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(cfg.host);
    factory.setPort(cfg.port);
    factory.setUsername(cfg.user);
    factory.setPassword(cfg.password);
    try {
      this.connection = factory.newConnection("itx-java");
    } catch (IOException | TimeoutException e) {
      throw new QueueException("failed to open RabbitMQ connection", e);
    }
    this.config = cfg;
  }

  private static String requireEnv(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank()) {
      throw new IllegalStateException("missing env var: " + name);
    }
    return v;
  }

  private static int envInt(String name, int defaultValue) {
    String v = System.getenv(name);
    return (v == null || v.isBlank()) ? defaultValue : Integer.parseInt(v);
  }

  @Override
  public MessageQueue createControlStandardQueue() {
    return new RabbitMessageQueue(connection, config.controlStandardQueue, config.maxConcurrency);
  }

  @Override
  public MessageQueue createControlPremiumQueue() {
    return new RabbitMessageQueue(connection, config.controlPremiumQueue, config.maxConcurrency);
  }

  @Override
  public MessageQueue createComputeStandardQueue() {
    return new RabbitMessageQueue(connection, config.computeStandardQueue, config.maxConcurrency);
  }

  @Override
  public MessageQueue createComputePremiumQueue() {
    return new RabbitMessageQueue(connection, config.computePremiumQueue, config.maxConcurrency);
  }
}
