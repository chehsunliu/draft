package io.github.chehsunliu.itx.contract.queue;

@FunctionalInterface
public interface MessageHandler {
  /**
   * Processes a single message body. Returning normally causes the queue to ack/delete; throwing
   * causes the queue to nack/reject so the broker can route the message to its DLQ.
   */
  void handle(String body) throws Exception;
}
