package io.github.chehsunliu.itx.contract.queue;

import java.util.concurrent.atomic.AtomicBoolean;

public interface MessageQueue {
  /** Publish a message to this queue. */
  void publish(String body);

  /**
   * Run the consumer loop, dispatching each message to {@code handler}. When {@code cancel} is
   * triggered the impl stops pulling new messages and waits for in-flight handlers to finish
   * (graceful drain) before returning.
   */
  void receive(MessageHandler handler, AtomicBoolean cancel);
}
