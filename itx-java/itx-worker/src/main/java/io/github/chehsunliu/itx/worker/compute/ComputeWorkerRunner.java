package io.github.chehsunliu.itx.worker.compute;

import io.github.chehsunliu.itx.contract.queue.MessageQueueFactory;
import io.github.chehsunliu.itx.worker.run.QueueRunner;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("compute")
public class ComputeWorkerRunner {

  @Bean
  QueueRunner computeQueueRunner(MessageQueueFactory factory, ComputeDispatcher dispatcher) {
    return new QueueRunner(
        List.of(factory.createComputeStandardQueue(), factory.createComputePremiumQueue()),
        dispatcher);
  }
}
