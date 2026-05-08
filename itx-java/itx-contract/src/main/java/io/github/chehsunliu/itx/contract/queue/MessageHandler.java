package io.github.chehsunliu.itx.contract.queue;

@FunctionalInterface
public interface MessageHandler {
  /**
   * Process a single message. Returning normally causes the queue to ack/delete the message;
   * throwing causes the queue to nack/reject so the broker can route to the DLQ.
   */
  void handle(String body) throws Exception;
}
