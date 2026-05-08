package io.github.chehsunliu.itx.worker.state;

import io.github.chehsunliu.itx.contract.email.EmailClient;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.impl.email.HttpEmailClient;
import io.github.chehsunliu.itx.impl.queue.rabbitmq.RabbitMessageQueueFactory;
import io.github.chehsunliu.itx.impl.queue.sqs.SqsMessageQueueFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class WorkerStateConfig {

  @Bean
  MessageQueueFactory messageQueueFactory() {
    String provider = System.getenv().getOrDefault("ITX_QUEUE_PROVIDER", "sqs");
    return switch (provider) {
      case "sqs" -> new SqsMessageQueueFactory();
      case "rabbitmq" -> new RabbitMessageQueueFactory();
      default -> throw new IllegalStateException("unknown ITX_QUEUE_PROVIDER: " + provider);
    };
  }

  @Configuration
  @Profile("control")
  public static class ControlBeans {

    @Bean
    EmailClient emailClient() {
      return HttpEmailClient.fromEnv();
    }
  }
}
