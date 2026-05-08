package io.github.chehsunliu.itx.backend.state;

import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppStateConfig {

  @Bean
  MessageQueueFactory messageQueueFactory() {
    String provider = System.getenv().getOrDefault("ITX_QUEUE_PROVIDER", "sqs");
    return switch (provider) {
      case "sqs" -> new SqsMessageQueueFactory();
      case "rabbitmq" -> new RabbitMessageQueueFactory();
      default -> throw new IllegalStateException("unknown ITX_QUEUE_PROVIDER: " + provider);
    };
  }

  @Bean(name = "controlStandardQueue")
  MessageQueue controlStandardQueue(MessageQueueFactory factory) {
    return factory.createControlStandardQueue();
  }
}
