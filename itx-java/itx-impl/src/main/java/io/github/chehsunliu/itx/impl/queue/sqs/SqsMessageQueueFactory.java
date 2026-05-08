package io.github.chehsunliu.itx.impl.queue.sqs;

import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import java.net.URI;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

public class SqsMessageQueueFactory implements MessageQueueFactory {

  private final SqsClient client;
  private final Config config;

  private record Config(
      String localEndpointUrl,
      int maxConcurrency,
      String controlStandardQueueUrl,
      String controlPremiumQueueUrl,
      String computeStandardQueueUrl,
      String computePremiumQueueUrl) {}

  public SqsMessageQueueFactory() {
    Config cfg =
        new Config(
            System.getenv("ITX_SQS_LOCAL_ENDPOINT_URL"),
            envInt("ITX_SQS_MAX_CONCURRENCY", 100),
            requireEnv("ITX_SQS_CONTROL_STANDARD_QUEUE_URL"),
            requireEnv("ITX_SQS_CONTROL_PREMIUM_QUEUE_URL"),
            requireEnv("ITX_SQS_COMPUTE_STANDARD_QUEUE_URL"),
            requireEnv("ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL"));

    SqsClientBuilder b = SqsClient.builder();
    if (cfg.localEndpointUrl != null && !cfg.localEndpointUrl.isBlank()) {
      b = b.endpointOverride(URI.create(cfg.localEndpointUrl));
    }
    this.client = b.build();
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
    return new SqsMessageQueue(client, config.controlStandardQueueUrl, config.maxConcurrency);
  }

  @Override
  public MessageQueue createControlPremiumQueue() {
    return new SqsMessageQueue(client, config.controlPremiumQueueUrl, config.maxConcurrency);
  }

  @Override
  public MessageQueue createComputeStandardQueue() {
    return new SqsMessageQueue(client, config.computeStandardQueueUrl, config.maxConcurrency);
  }

  @Override
  public MessageQueue createComputePremiumQueue() {
    return new SqsMessageQueue(client, config.computePremiumQueueUrl, config.maxConcurrency);
  }
}
