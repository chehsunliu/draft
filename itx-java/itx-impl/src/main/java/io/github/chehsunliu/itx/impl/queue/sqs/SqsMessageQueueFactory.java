package io.github.chehsunliu.itx.impl.queue.sqs;

import static io.github.chehsunliu.itx.impl.Env.envInt;
import static io.github.chehsunliu.itx.impl.Env.requireEnv;

import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@RequiredArgsConstructor
public final class SqsMessageQueueFactory implements MessageQueueFactory {
  private final SqsClient client;
  private final int maxConcurrency;
  private final String controlStandardQueueUrl;
  private final String controlPremiumQueueUrl;
  private final String computeStandardQueueUrl;
  private final String computePremiumQueueUrl;

  public static SqsMessageQueueFactory fromEnv() {
    String endpoint = System.getenv("ITX_SQS_LOCAL_ENDPOINT_URL");
    SqsClientBuilder builder = SqsClient.builder();
    if (endpoint != null && !endpoint.isBlank()) {
      builder.endpointOverride(URI.create(endpoint));
    }
    return new SqsMessageQueueFactory(
        builder.build(),
        envInt("ITX_SQS_MAX_CONCURRENCY", 100),
        requireEnv("ITX_SQS_CONTROL_STANDARD_QUEUE_URL"),
        requireEnv("ITX_SQS_CONTROL_PREMIUM_QUEUE_URL"),
        requireEnv("ITX_SQS_COMPUTE_STANDARD_QUEUE_URL"),
        requireEnv("ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL"));
  }

  @Override
  public MessageQueue createControlStandardQueue() {
    return new SqsMessageQueue(client, controlStandardQueueUrl, maxConcurrency);
  }

  @Override
  public MessageQueue createControlPremiumQueue() {
    return new SqsMessageQueue(client, controlPremiumQueueUrl, maxConcurrency);
  }

  @Override
  public MessageQueue createComputeStandardQueue() {
    return new SqsMessageQueue(client, computeStandardQueueUrl, maxConcurrency);
  }

  @Override
  public MessageQueue createComputePremiumQueue() {
    return new SqsMessageQueue(client, computePremiumQueueUrl, maxConcurrency);
  }
}
