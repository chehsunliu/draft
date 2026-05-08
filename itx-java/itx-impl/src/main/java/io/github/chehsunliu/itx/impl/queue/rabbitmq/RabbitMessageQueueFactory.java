package io.github.chehsunliu.itx.impl.queue.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.contract.queue.QueueException;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "itx.queue.provider", havingValue = "rabbitmq")
public class RabbitMessageQueueFactory implements MessageQueueFactory {

  private final Connection connection;
  private final RabbitProperties props;

  public RabbitMessageQueueFactory(RabbitProperties props) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(props.getHost());
    factory.setPort(props.getPort());
    factory.setUsername(props.getUser());
    factory.setPassword(props.getPassword());
    try {
      this.connection = factory.newConnection("itx-java");
    } catch (IOException | TimeoutException e) {
      throw new QueueException("failed to open RabbitMQ connection", e);
    }
    this.props = props;
  }

  @PreDestroy
  void close() throws IOException {
    connection.close();
  }

  @Override
  public MessageQueue createControlStandardQueue() {
    return new RabbitMessageQueue(
        connection, props.getControlStandardQueue(), props.getMaxConcurrency());
  }

  @Override
  public MessageQueue createControlPremiumQueue() {
    return new RabbitMessageQueue(
        connection, props.getControlPremiumQueue(), props.getMaxConcurrency());
  }

  @Override
  public MessageQueue createComputeStandardQueue() {
    return new RabbitMessageQueue(
        connection, props.getComputeStandardQueue(), props.getMaxConcurrency());
  }

  @Override
  public MessageQueue createComputePremiumQueue() {
    return new RabbitMessageQueue(
        connection, props.getComputePremiumQueue(), props.getMaxConcurrency());
  }
}
