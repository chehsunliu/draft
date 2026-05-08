package io.github.chehsunliu.itx.backend.state;

import io.github.chehsunliu.itx.contract.queue.MessageQueue;
import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppStateConfig {

  @Bean(name = "controlStandardQueue")
  MessageQueue controlStandardQueue(MessageQueueFactory factory) {
    return factory.createControlStandardQueue();
  }
}
