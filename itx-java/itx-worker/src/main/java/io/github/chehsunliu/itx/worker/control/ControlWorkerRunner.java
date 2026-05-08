package io.github.chehsunliu.itx.worker.control;

import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.worker.run.QueueRunner;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "itx.worker.mode", havingValue = "control")
public class ControlWorkerRunner {

  @Bean
  QueueRunner controlQueueRunner(MessageQueueFactory factory, ControlDispatcher dispatcher) {
    return new QueueRunner(
        List.of(factory.createControlStandardQueue(), factory.createControlPremiumQueue()),
        dispatcher);
  }
}
