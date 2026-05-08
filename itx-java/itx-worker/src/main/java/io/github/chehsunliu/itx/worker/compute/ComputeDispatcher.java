package io.github.chehsunliu.itx.worker.compute;

import io.github.chehsunliu.itx.contract.queue.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ComputeDispatcher implements MessageHandler {
  private static final Logger log = LoggerFactory.getLogger(ComputeDispatcher.class);

  @Override
  public void handle(String body) {
    // No compute-plane message types yet — just log and ack so the queue stays drained.
    log.info("compute message received (no handler yet) body={}", body);
  }
}
