package io.github.chehsunliu.itx.worker.compute;

import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "itx.worker.mode", havingValue = "compute")
public class ComputeDispatcher implements MessageHandler {
  @Override
  public void handle(String body) {
    // No compute-plane message types yet — just log and ack so the queue stays drained.
    log.info("compute message received (no handler yet) body={}", body);
  }
}
