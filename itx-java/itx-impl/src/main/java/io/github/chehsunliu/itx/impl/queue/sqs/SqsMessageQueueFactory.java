package io.github.chehsunliu.itx.impl.queue.sqs;

import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Component
@ConditionalOnProperty(name = "itx.queue.provider", havingValue = "sqs")
public class SqsMessageQueueFactory implements MessageQueueFactory {

  private final SqsClient client;
  private final SqsProperties props;

  public SqsMessageQueueFactory(SqsProperties props) {
    SqsClientBuilder b = SqsClient.builder();
    if (props.getLocalEndpointUrl() != null && !props.getLocalEndpointUrl().isBlank()) {
      b = b.endpointOverride(URI.create(props.getLocalEndpointUrl()));
    }
    this.client = b.build();
    this.props = props;
  }

  @PreDestroy
  void close() {
    client.close();
  }

  @Override
  public MessageQueue createControlStandardQueue() {
    return new SqsMessageQueue(
        client, props.getControlStandardQueueUrl(), props.getMaxConcurrency());
  }

  @Override
  public MessageQueue createControlPremiumQueue() {
    return new SqsMessageQueue(
        client, props.getControlPremiumQueueUrl(), props.getMaxConcurrency());
  }

  @Override
  public MessageQueue createComputeStandardQueue() {
    return new SqsMessageQueue(
        client, props.getComputeStandardQueueUrl(), props.getMaxConcurrency());
  }

  @Override
  public MessageQueue createComputePremiumQueue() {
    return new SqsMessageQueue(
        client, props.getComputePremiumQueueUrl(), props.getMaxConcurrency());
  }
}
