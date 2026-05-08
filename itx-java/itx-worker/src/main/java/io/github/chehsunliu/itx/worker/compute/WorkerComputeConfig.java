package io.github.chehsunliu.itx.worker.compute;

import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.QueueProperties;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitProperties;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsProperties;
import io.github.chehsunliu.itx.worker.run.QueueRunner;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WorkerComputeConfig {

  private final QueueProperties queueProps;
  private final SqsProperties sqsProps;
  private final RabbitProperties rabbitProps;

  @Bean
  MessageQueueFactory messageQueueFactory() {
    return switch (queueProps.getProvider()) {
      case "sqs" -> new SqsMessageQueueFactory(sqsProps);
      case "rabbitmq" -> new RabbitMessageQueueFactory(rabbitProps);
      default ->
          throw new IllegalStateException(
              "unknown itx.queue.provider: " + queueProps.getProvider());
    };
  }

  @Bean
  ComputeDispatcher computeDispatcher() {
    return new ComputeDispatcher();
  }

  @Bean
  QueueRunner computeQueueRunner(MessageQueueFactory factory, ComputeDispatcher dispatcher) {
    return new QueueRunner(
        List.of(factory.createComputeStandardQueue(), factory.createComputePremiumQueue()),
        dispatcher);
  }
}
